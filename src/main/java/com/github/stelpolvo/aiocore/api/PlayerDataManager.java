package com.github.stelpolvo.aiocore.api;

import com.github.stelpolvo.aiocore.api.data.PlayerData;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;

public interface PlayerDataManager extends Listener {
    default PlayerData getByName(String playerName) {
        throw new RuntimeException("Not implemented");
    }

    default PlayerData getByUUID(UUID uuid) {
        throw new RuntimeException("Not implemented");
    }

    default Map<UUID, PlayerData> getPlayerData() {
        throw new RuntimeException("Not implemented");
    }

    default void save(PlayerData data) {
        throw new RuntimeException("Not implemented");
    }

    default void saveAll(){
        throw new RuntimeException("Not implemented");
    }


}
