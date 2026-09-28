package com.github.stelpolvo.aiocore.api;

import org.bukkit.plugin.java.JavaPlugin;

public interface AiO {
    String NAME = "aio";

    void saveData();

    JavaPlugin getJavaPlugin();

    Messenger getMessenger();

    EconomyManager getEconomyManager();

    ChatManager getChatManager();

    PlayerDataManager getPlayerDataManager();

}
