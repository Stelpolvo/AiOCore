package com.github.stelpolvo.aiocore.command.handler;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.StashManager;
import com.github.stelpolvo.aiocore.command.CommandAPI;
import com.github.stelpolvo.aiocore.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * /aio stash                  打开自己的第 1 页
 * /aio stash &lt;page&gt;           打开自己的第 page 页
 * /aio stash &lt;page&gt; &lt;player&gt;  查看指定玩家的第 page 页（仅 aio.command.admin.stash.view）
 * <p>
 * 页码权限逐页判定：aio.command.def.stash.page.N，max-pages 只限制上限。
 */
public class StashHandler implements CommandAPI {

    public static final String PERM_BASE = "aio.command.def.stash";
    public static final String PERM_PAGE_PREFIX = PERM_BASE + ".page.";
    public static final String PERM_ADMIN_VIEW = "aio.command.admin.stash.view";

    private final StashManager stash;
    private final Messenger messenger;
    private final Logger logger;
    private final int maxPages;
    private final List<String> cachedPages = new ArrayList<>();

    public StashHandler(StashManager stash, Messenger messenger, Logger logger, int maxPages) {
        this.stash = stash;
        this.messenger = messenger;
        this.logger = logger;
        this.maxPages = Math.max(1, maxPages);
        for (int page = 1; page <= this.maxPages; page++) {
            cachedPages.add(String.valueOf(page));
        }
    }

    @Override
    public boolean onPlayer(Player player, String[] args) {
        return onCommand(player, args);
    }

    @Override
    public boolean onConsole(CommandSender sender, String[] args) {
        return onCommand(sender, args);
    }

    public boolean onCommand(CommandSender sender, String[] args) {
        if (!stash.isEnabled()) {
            messenger.send(sender, Messenger.STASH_DISABLED);
            return true;
        }
        int length = args == null ? 0 : args.length;
        try {
            if (length >= 3) {
                return view(sender, args[1], args[2]);
            }
            if (length == 2) {
                return self(sender, args[1]);
            }
            return self(sender, "1");
        } catch (NumberFormatException e) {
            messenger.send(sender, Messenger.NUMBER_FORMAT_EXCEPTION);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Unexpected error while handling /aio stash", e);
        }
        return true;
    }

    /**
     * 打开自己的仓库。
     */
    public boolean self(CommandSender sender, String pageArg) {
        if (!(sender instanceof Player player)) {
            messenger.send(sender, Messenger.IS_PLAYER_COMMAND);
            return true;
        }
        if (!player.hasPermission(PERM_BASE)) {
            messenger.send(player, Messenger.NO_PERMISSION, "permission", PERM_BASE);
            return true;
        }
        int page = parsePage(player, pageArg);
        if (page < 0) {
            return true;
        }
        if (!player.hasPermission(PERM_PAGE_PREFIX + page)) {
            messenger.send(player, Messenger.STASH_LOCKED, "page", page);
            return true;
        }
        stash.open(player, page);
        return true;
    }

    /**
     * 管理员查看指定玩家的仓库，不检查玩家自己的页码权限。
     */
    public boolean view(CommandSender sender, String pageArg, String targetName) {
        if (!(sender instanceof Player viewer)) {
            messenger.send(sender, Messenger.IS_PLAYER_COMMAND);
            return true;
        }
        if (!sender.hasPermission(PERM_ADMIN_VIEW)) {
            messenger.send(sender, Messenger.NO_PERMISSION, "permission", PERM_ADMIN_VIEW);
            return true;
        }
        int page = parsePage(sender, pageArg);
        if (page < 0) {
            return true;
        }
        OfflinePlayer target = resolveTarget(targetName);
        if (target == null) {
            messenger.send(sender, Messenger.PLAYER_NOT_EXIST);
            return true;
        }
        String name = target.getName() == null ? targetName : target.getName();
        stash.open(viewer, target.getUniqueId(), name, page);
        return true;
    }

    @Override
    public List<String> onPlayerTab(Player player, String[] args) {
        return onTab(player, args);
    }

    @Override
    public List<String> onConsoleTab(CommandSender sender, String[] args) {
        return onTab(sender, args);
    }

    public List<String> onTab(CommandSender sender, String[] args) {
        if (args == null || args.length < 2) {
            return List.of();
        }
        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        // /aio stash <page> — 补全能打开且未超出上限的页码
        if (args.length == 2) {
            if (!sender.hasPermission(PERM_BASE)) {
                return List.of();
            }
            return cachedPages.stream()
                    .filter(page -> page.contains(current))
                    .filter(page -> sender.hasPermission(PERM_PAGE_PREFIX + page))
                    .toList();
        }
        // /aio stash <page> <player> — 只有管理员才能补玩家名
        if (args.length == 3 && sender.hasPermission(PERM_ADMIN_VIEW)) {
            return PlayerUtil.getAllPlayerNames(current);
        }
        return List.of();
    }

    /**
     * 解析 1 起算的页码，非法时提示并返回 -1。
     * 非数字或小于 1 提示参数非法，超出 max-pages 提示未解锁。
     */
    private int parsePage(CommandSender sender, String pageArg) {
        int page;
        try {
            page = Integer.parseInt(pageArg == null ? "" : pageArg.trim());
        } catch (NumberFormatException e) {
            messenger.send(sender, Messenger.STASH_INVALID_PAGE,
                    "page", pageArg == null ? "" : pageArg);
            return -1;
        }
        if (page < 1) {
            messenger.send(sender, Messenger.STASH_INVALID_PAGE, "page", page);
            return -1;
        }
        if (page > maxPages) {
            messenger.send(sender, Messenger.STASH_LOCKED, "page", page);
            return -1;
        }
        return page;
    }

    /**
     * 在线优先，其次历史玩家。
     */
    private OfflinePlayer resolveTarget(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        UUID uuid = PlayerUtil.getKnownUUID(name);
        return uuid == null ? null : Bukkit.getOfflinePlayer(uuid);
    }
}
