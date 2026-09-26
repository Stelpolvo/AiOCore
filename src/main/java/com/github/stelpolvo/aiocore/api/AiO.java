package com.github.stelpolvo.aiocore.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public interface AiO {
    String NAME = "aio";

    default void saveData(){}

    default JavaPlugin getJavaPlugin() {
        return null;
    }

    default PlaceholderExpansion getPlaceholderExpansion() {
        return null;
    }

    default Messenger getMessenger() {
        return null;
    }

    default EconomyManager getEconomyManager() {
        return null;
    }

    default ChatManager getChatManager() {
        return null;
    }

    default PlayerDataManager getPlayerDataManager(){ return null; }

    default PanelManager getPanelManager() {
        return null;
    }

    default PluginManager getPluginManager() {
        return null;
    }

}
