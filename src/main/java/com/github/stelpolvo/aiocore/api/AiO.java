package com.github.stelpolvo.aiocore.api;

import org.bukkit.plugin.PluginManager;

public interface AiO {
    default EconomyManager getEconomyManager() {
        return null;
    }

    default PanelManager getPanelManager() {
        return null;
    }

    default PluginManager getPluginManager() {
        return null;
    }
}
