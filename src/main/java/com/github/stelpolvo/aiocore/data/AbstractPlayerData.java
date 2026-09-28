package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.github.stelpolvo.aiocore.api.data.EconomyData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
import com.github.stelpolvo.aiocore.api.data.StashData;
import com.github.stelpolvo.aiocore.model.stash.StashManagerImpl;

import java.util.UUID;
import java.util.logging.Logger;

public abstract class AbstractPlayerData implements PlayerData {
    protected final UUID uuid;
    protected EconomyData economyData;
    protected ChatData chatData;
    protected StashData stashData;

    protected AbstractPlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public StashData getStashData() {
        if (stashData == null) {
            // 仓库实例由 StashManager 统一持有，这里兜底绑定一次
            StashManagerImpl stash = StashManagerImpl.current();
            if (stash == null) {
                Logger.getLogger(AbstractPlayerData.class.getName())
                        .warning("Stash manager is not initialized yet, "
                                + "stash data of " + uuid + " is unavailable");
                return null;
            }
            stashData = stash.getStashData(uuid);
        }
        return stashData;
    }

    @Override
    public void setStashData(StashData data) {
        this.stashData = data;
    }
}
