package com.github.stelpolvo.aiocore.api;

import com.github.stelpolvo.aiocore.api.data.PlayerData;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;

public interface PlayerDataManager extends Listener {
    PlayerData getByName(String playerName);

    PlayerData getByUUID(UUID uuid);

    Map<UUID, PlayerData> getPlayerData();

    boolean save(PlayerData data);

    void saveAll();

    void disable();
}
