package com.github.stelpolvo.aiocore.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.List;

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
}
