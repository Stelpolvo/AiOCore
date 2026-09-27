package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.github.stelpolvo.aiocore.api.data.EconomyData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;

import java.util.UUID;

public abstract class AbstractPlayerData implements PlayerData {
    protected final UUID uuid;
    protected EconomyData economyData;
    protected ChatData chatData;

    protected AbstractPlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
