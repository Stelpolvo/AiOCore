package com.github.stelpolvo.aiocore.model.stash;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.StashManager;
import com.github.stelpolvo.aiocore.api.data.StashData;
import com.github.stelpolvo.aiocore.api.storage.StashStorage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StashManagerImpl implements StashManager {

    public static final int DEFAULT_PAGE_SIZE = 54;
    public static final int DEFAULT_MAX_PAGES = 3;

    private static final long IDLE_EVICT_MILLIS = 5 * 60 * 1000L;

    private static StashManagerImpl instance;

    private final Logger logger;
    private final StashStorage storage;
    private final Messenger messenger;
    private final int pageSize;
    private final int maxPages;
    private final Map<UUID, StashDataImpl> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAccess = new ConcurrentHashMap<>();

    public StashManagerImpl(ConfigurationSection config, StashStorage storage,
                            Messenger messenger, Logger logger) {
        this.storage = storage;
        this.messenger = messenger;
        this.logger = logger;
        this.pageSize = config == null
                ? DEFAULT_PAGE_SIZE
                : config.getInt("page-size", DEFAULT_PAGE_SIZE);
        this.maxPages = Math.max(1, config == null
                ? DEFAULT_MAX_PAGES
                : config.getInt("max-pages", DEFAULT_MAX_PAGES));
        if (this.pageSize <= 0 || this.pageSize % 9 != 0) {
            logger.warning("Invalid stash page-size " + this.pageSize
                    + ", falling back to " + DEFAULT_PAGE_SIZE);
        }
        instance = this;
    }

    public static StashManagerImpl current() {
        return instance;
    }

    @Override
    public int getMaxPages() {
        return maxPages;
    }

    @Override
    public boolean isEnabled() {
        return storage != null;
    }

    @Override
    public StashData getStashData(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return touch(uuid);
    }

    @Override
    public void open(Player viewer, int page) {
        open(viewer, viewer.getUniqueId(), viewer.getName(), page);
    }

    @Override
    public void open(Player viewer, UUID owner, String ownerName, int page) {
        if (page < 1) {
            messenger.send(viewer, Messenger.STASH_INVALID_PAGE, "page", page);
            return;
        }
        if (page > maxPages) {
            messenger.send(viewer, Messenger.STASH_LOCKED, "page", page);
            return;
        }
        try {
            int index = page - 1;
            StashDataImpl data = touch(owner);
            if (!data.isPageLoaded(index)) {
                data.load(index);
            }
            Inventory inv = data.get(index);
            viewer.openInventory(inv);
            if (!viewer.getUniqueId().equals(owner)) {
                messenger.send(viewer, Messenger.STASH_VIEW_READONLY, "player", ownerName);
            }
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to open stash page " + page
                    + " of " + owner + " for " + viewer.getUniqueId(), e);
            messenger.send(viewer, Messenger.STASH_OPEN_FAILED);
        }
    }

    @Override
    public void save(UUID uuid, int page) {
        StashDataImpl data = cache.get(uuid);
        if (data == null) {
            return;
        }
        data.save(page);
        storage.flush();
    }

    public void save(Player player, int page) {
        save(player.getUniqueId(), page);
    }

    public void save(Player player) {
        save(player.getUniqueId());
    }

    @Override
    public void save(UUID uuid) {
        StashDataImpl data = cache.get(uuid);
        if (data == null) {
            return;
        }
        data.save();
        storage.flush();
    }

    @Override
    public void saveAll() {
        for (Map.Entry<UUID, StashDataImpl> entry : cache.entrySet()) {
            try {
                entry.getValue().save();
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Failed to save stash of "
                        + entry.getKey() + " during saveAll", e);
            }
        }
        storage.flush();
        evictIdle();
    }

    @Override
    public void evict(UUID uuid) {
        StashDataImpl data = cache.remove(uuid);
        lastAccess.remove(uuid);
        if (data != null) {
            data.release();
        }
        if (storage instanceof JsonStashStorage json) {
            json.unload(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StashHolder holder)) {
            return;
        }
        UUID owner = holder.getOwner();
        StashDataImpl data = cache.get(owner);
        if (data == null) {
            return;
        }
        save(owner, holder.getPage());
        // 离线玩家的仓库查看结束就卸载，在线玩家留到退出时统一处理
        if (Bukkit.getPlayer(owner) == null) {
            evict(owner);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StashHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (player.getUniqueId().equals(holder.getOwner())) {
            return;
        }
        event.setCancelled(true);
        messenger.send(player, Messenger.STASH_VIEW_READONLY, "player", holder.getOwner());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerQuitEvent event) {
        evict(event.getPlayer().getUniqueId());
    }

    public void evictIdle() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : lastAccess.entrySet()) {
            UUID uuid = entry.getKey();
            if (now - entry.getValue() < IDLE_EVICT_MILLIS) {
                continue;
            }
            if (Bukkit.getPlayer(uuid) != null) {
                continue;
            }
            evict(uuid);
        }
    }

    private StashDataImpl touch(UUID uuid) {
        lastAccess.put(uuid, System.currentTimeMillis());
        return cache.computeIfAbsent(uuid,
                key -> new StashDataImpl(key, pageSize, storage, logger));
    }
}
