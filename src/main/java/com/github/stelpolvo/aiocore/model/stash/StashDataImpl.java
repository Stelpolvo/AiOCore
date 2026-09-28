package com.github.stelpolvo.aiocore.model.stash;

import com.github.stelpolvo.aiocore.api.StashManager;
import com.github.stelpolvo.aiocore.api.data.StashData;
import com.github.stelpolvo.aiocore.api.storage.StashStorage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StashDataImpl implements StashData {

    private static final int FORMAT_VERSION = 1;

    private final UUID uuid;
    private final int pageSize;
    private final StashStorage storage;
    private final Logger logger;
    private final Map<Integer, Inventory> pages = new ConcurrentHashMap<>();
    private final Set<Integer> loadedPages = ConcurrentHashMap.newKeySet();
    private final Set<Integer> modifiedPages = ConcurrentHashMap.newKeySet();

    public StashDataImpl(UUID uuid, int pageSize, StashStorage storage, Logger logger) {
        this.uuid = uuid;
        this.pageSize = pageSize;
        this.storage = storage;
        this.logger = logger;
    }

    @Override
    public UUID getOwner() {
        return uuid;
    }

    @Override
    public Inventory get(int page) {
        return pages.computeIfAbsent(page, this::createPage);
    }

    public int getPageSize() {
        return pageSize;
    }

    @Override
    public boolean isPageLoaded(int page) {
        return loadedPages.contains(page);
    }

    @Override
    public Set<Integer> getLoadedPages() {
        return new HashSet<>(loadedPages);
    }

    /**
     * 从存储载入一页，已经载入过或没有数据时直接返回。
     */
    public void load(int page) {
        if (loadedPages.contains(page)) {
            return;
        }
        loadedPages.add(page);
        try {
            JsonObject root = storage.load(uuid, page);
            if (root != null) {
                loadFromJson(page, root);
            }
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to load stash page " + page
                    + " for " + uuid, e);
        }
    }

    @Override
    public JsonObject toJson(int page) {
        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);

        Inventory inv = pages.get(page);
        if (inv == null) {
            root.add("items", new JsonObject());
            return root;
        }

        JsonObject items = new JsonObject();
        ItemStack[] contents = inv.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;
            String encoded = serializeItem(item);
            if (encoded != null) {
                items.addProperty(String.valueOf(slot), encoded);
            }
        }
        root.add("items", items);
        return root;
    }

    @Override
    public void loadFromJson(int page, JsonObject root) {
        Inventory inv = get(page);
        inv.clear();
        loadedPages.add(page);

        if (root == null || !root.has("items") || !root.get("items").isJsonObject()) {
            return;
        }

        JsonObject items = root.getAsJsonObject("items");
        for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
            int slot;
            try {
                slot = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            if (slot < 0 || slot >= pageSize) continue;

            String encoded;
            try {
                encoded = entry.getValue().getAsString();
            } catch (Exception e) {
                continue;
            }

            ItemStack item = deserializeItem(encoded);
            if (item != null) {
                inv.setItem(slot, item);
            }
        }
        modifiedPages.remove(page);
    }

    @Override
    public void markModified(int page) {
        modifiedPages.add(page);
    }

    @Override
    public boolean isModified(int page) {
        return modifiedPages.contains(page);
    }

    @Override
    public void markSaved(int page) {
        modifiedPages.remove(page);
    }

    @Override
    public boolean isDirty() {
        return !modifiedPages.isEmpty();
    }

    public boolean save() {
        boolean allSaved = true;
        for (int page : new HashSet<>(modifiedPages)) {
            if (save(page)) {
                markSaved(page);
            } else {
                allSaved = false;
            }
        }
        return allSaved;
    }

    public boolean save(int page) {
        try {
            JsonObject root = toJson(page);
            if (!storage.save(uuid, page, root)) {
                return false;
            }
            markSaved(page);
            return true;
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to save stash page " + page
                    + " for " + uuid, e);
            return false;
        }
    }

    public void release() {
        if (isDirty()) {
            save();
        }
        storage.flush();
        pages.clear();
        loadedPages.clear();
        modifiedPages.clear();
    }

    private Inventory createPage(int page) {
        StashManager.StashHolder holder = new StashHolderImpl(uuid, page);
        Inventory inv = Bukkit.createInventory(
                holder,
                pageSize
        );
        holder.setInventory(inv);
        return inv;
    }

    private String serializeItem(ItemStack item) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos)) {
            out.writeObject(item);
            out.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to serialize item "
                    + item.getType() + " in stash of " + uuid, e);
            return null;
        }
    }

    private ItemStack deserializeItem(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 BukkitObjectInputStream in = new BukkitObjectInputStream(bais)) {
                Object obj = in.readObject();
                return obj instanceof ItemStack item ? item : null;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Skipping unreadable stash item for " + uuid, e);
            return null;
        }
    }
}
