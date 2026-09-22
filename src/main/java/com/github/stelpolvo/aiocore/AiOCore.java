package com.github.stelpolvo.aiocore;

import com.github.stelpolvo.aiocore.api.AiO;
import com.github.stelpolvo.aiocore.api.EconomyManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AiOCore extends JavaPlugin implements AiO {

    @Override
    public void onEnable() {
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public EconomyManager getEconomyManager() {
        return null;
    }
}
