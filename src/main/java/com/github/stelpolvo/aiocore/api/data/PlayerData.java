package com.github.stelpolvo.aiocore.api.data;

import java.util.UUID;

public interface PlayerData {
    UUID getUUID();
    EconomyData getEconomyData();
    void setEconomyData(EconomyData data);
    ChatData getChatData();
    void setChatData(ChatData data);
}