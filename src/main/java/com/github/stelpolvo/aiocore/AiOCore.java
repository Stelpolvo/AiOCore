package com.github.stelpolvo.aiocore;

import com.github.stelpolvo.aiocore.api.*;
import com.github.stelpolvo.aiocore.command.MainCommand;
import com.github.stelpolvo.aiocore.data.YamlPlayerDataManager;
import com.github.stelpolvo.aiocore.model.chat.ChatManagerImpl;
import com.github.stelpolvo.aiocore.model.economy.EconomyManagerImpl;
import com.github.stelpolvo.aiocore.model.message.MessengerImpl;
import com.github.stelpolvo.aiocore.model.placeholder.AiOHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public final class AiOCore extends JavaPlugin implements AiO {
    private PlaceholderExpansion placeholder;
    private EconomyManager economyManager;
    private ChatManager chatManager;
    private PlayerDataManager playerDataManager;
    private Messenger messenger;

    private BukkitTask task;
    @Override
    public void onEnable() {
        // Plugin startup logic
        loadConfig();
        ServicesManager servicesManager = Bukkit.getServicesManager();
        servicesManager.register(AiO.class, this, this, ServicePriority.Highest);
        EconomyManager.AiOEconomy vaultEconomy = economyManager.getVaultEconomy();
        if (vaultEconomy != null) {
            servicesManager.register(Economy.class, vaultEconomy.getEconomy(), this, ServicePriority.Highest);
        }
        MainCommand.init(this);
    }

    private void loadConfig(){
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        // messenger
        if (this.messenger == null){
            this.messenger = new MessengerImpl(config);
        }else {
            this.messenger.load(config);
        }
        // player data
        this.playerDataManager = new YamlPlayerDataManager(new File(getDataFolder(), config.getString("settings.save-path", "/data")), getLogger(), messenger);
        Bukkit.getPluginManager().registerEvents(this.playerDataManager, this);
        // economy
        this.economyManager = new EconomyManagerImpl();
        this.economyManager.load(config.getConfigurationSection("economy"), playerDataManager, getLogger(), messenger);
        // chat
        this.chatManager = new ChatManagerImpl(this.playerDataManager, config.getConfigurationSection("chat"));
        Bukkit.getPluginManager().registerEvents(this.chatManager, this);

        this.placeholder = new AiOHook(this);
        this.placeholder.register();
        if (task != null){
            task.cancel();
            task = null;
        }
        long updateDuration = Math.max(1, config.getLong("settings.save-interval-seconds"))*20;
        task = new BukkitRunnable() {
            public void run() {
                saveData();
            }
        }.runTaskTimer(this, updateDuration, updateDuration);
        new AiOHook(this).register();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        task.cancel();
        task = null;
        saveData();
    }

    @Override
    public void saveData(){
        this.playerDataManager.saveAll();
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public PlaceholderExpansion getPlaceholderExpansion() {
        return placeholder;
    }

    @Override
    public Messenger getMessenger() {
        return messenger;
    }

    @Override
    public EconomyManager getEconomyManager() {
        return this.economyManager;
    }

    @Override
    public ChatManager getChatManager() {
        return this.chatManager;
    }

    @Override
    public PlayerDataManager getPlayerDataManager(){
        return this.playerDataManager;
    }


}
