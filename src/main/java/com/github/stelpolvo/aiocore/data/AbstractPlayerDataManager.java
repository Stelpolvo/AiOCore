package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.PlayerData;

import java.io.File;
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

    /**
     * 仓库文件的落盘目录，仅 yml 数据源需要。
     */
    public File getStashFolder() {
        return null;
    }
}
