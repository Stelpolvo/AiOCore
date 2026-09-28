package com.github.stelpolvo.aiocore.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PlayerUtil {
    public static List<String> getAllPlayerNames(String regex) {
        if (regex == null){
            return Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).toList();
        } else {
            return Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).filter(name -> name != null && name.contains(regex)).toList();
        }
    }

    public static OfflinePlayer getIfPlayBefore(String playerName){
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player.hasPlayedBefore()){
            return player;
        }
        return null;
    }

    /**
     * 在线优先，其次取历史玩家 UUID，都没有返回 null。
     * 不查 Mojang，避免主线程阻塞。
     */
    public static UUID getKnownUUID(String playerName){
        if (playerName == null || playerName.isBlank()){
            return null;
        }
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null){
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }
}
