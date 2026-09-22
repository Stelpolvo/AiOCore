package com.github.stelpolvo.aiocore.api;

import com.github.stelpolvo.aiocore.api.data.PlayerData;

import java.util.Map;
import java.util.UUID;

public interface PlayerDataManager {
    default PlayerData getByName(String playerName) {
        throw new RuntimeException("Not implemented");
    }

    default PlayerData getUUID(UUID uuid) {
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
