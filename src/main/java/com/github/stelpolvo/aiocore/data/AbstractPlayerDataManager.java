package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public abstract class AbstractPlayerDataManager implements PlayerDataManager {
    protected final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    protected final Logger logger;
    protected final Messenger messenger;
    public AbstractPlayerDataManager(Logger logger, Messenger messenger) {
        this.logger = logger;
        this.messenger = messenger;
    }
}
