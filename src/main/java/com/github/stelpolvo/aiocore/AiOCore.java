package com.github.stelpolvo.aiocore;

import com.github.stelpolvo.aiocore.api.*;
import com.github.stelpolvo.aiocore.api.storage.StashStorage;
import com.github.stelpolvo.aiocore.command.MainCommand;
import com.github.stelpolvo.aiocore.data.MySQLPlayerDataManager;
import com.github.stelpolvo.aiocore.data.SQLitePlayerDataManager;
import com.github.stelpolvo.aiocore.data.YamlPlayerDataManager;
import com.github.stelpolvo.aiocore.model.chat.ChatManagerImpl;
import com.github.stelpolvo.aiocore.model.economy.EconomyManagerImpl;
import com.github.stelpolvo.aiocore.model.message.MessengerImpl;
import com.github.stelpolvo.aiocore.model.placeholder.AiOHook;
import com.github.stelpolvo.aiocore.model.stash.StashManagerImpl;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

public final class AiOCore extends JavaPlugin implements AiO {
    private PlaceholderExpansion placeholder;
    private EconomyManager economyManager;
    private ChatManager chatManager;
    private StashManager stashManager;
    private PlayerDataManager playerDataManager;
    private Messenger messenger;

    private BukkitTask task;
    private BukkitTask stashTask;
    @Override
    public void onEnable() {
        // Plugin startup logic
        loadConfig();
        ServicesManager servicesManager = Bukkit.getServicesManager();
        servicesManager.register(AiO.class, this, this, ServicePriority.Highest);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
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
        ConfigurationSection storageSec = config.getConfigurationSection("settings.storage");
        if (storageSec != null) {
            switch (config.getString("settings.storage.type", "yml").toLowerCase()){
                case "sqlite":
                    this.playerDataManager = new SQLitePlayerDataManager(this, storageSec, getLogger(), this.messenger);
                    break;
                case "mysql":
                    this.playerDataManager = new MySQLPlayerDataManager(storageSec, getLogger(), this.messenger);
                    break;
                default:
                case "yaml":
                case "yml":
                    this.playerDataManager = new YamlPlayerDataManager(new File(getDataFolder(), storageSec.getString("url", "/data")), getLogger(), messenger);
                    break;
            }
        }else {
            this.playerDataManager = new YamlPlayerDataManager(new File(getDataFolder(),"/data"), getLogger(), messenger);
        }
        Bukkit.getPluginManager().registerEvents(this.playerDataManager, this);
        // stash
        ConfigurationSection stashSec = config.getConfigurationSection("stash");
        this.stashManager = new StashManagerImpl(
                stashSec,
                StashStorage.create(this.playerDataManager, getLogger()),
                this.messenger,
                getLogger());
        Bukkit.getPluginManager().registerEvents(this.stashManager, this);
        // economy
        this.economyManager = new EconomyManagerImpl();
        this.economyManager.load(config.getConfigurationSection("economy"), playerDataManager, getLogger(), messenger);
        // chat
        this.chatManager = new ChatManagerImpl(this, this.playerDataManager, config.getConfigurationSection("chat"), getLogger());
        Bukkit.getPluginManager().registerEvents(this.chatManager, this);

        if (task != null){
            task.cancel();
            task = null;
        }
        long updateDuration = Math.max(1, config.getLong("settings.save-interval-seconds"))*20;
        long stashDuration = Math.max(1, config.getLong("stash.save-interval-seconds", 10))*20;
        task = new BukkitRunnable() {
            public void run() {

                saveData();
            }
        }.runTaskTimer(this, updateDuration, updateDuration);
        stashTask = new BukkitRunnable() {
            public void run() {
                stashManager.saveAll();
            }
        }.runTaskTimer(this, stashDuration, stashDuration);
        this.placeholder = new AiOHook(this);
        this.placeholder.register();
        try {
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this.chatManager);
        }catch (IllegalArgumentException ignore){

        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        task.cancel();
        task = null;
        if (stashTask != null){
            stashTask.cancel();
            stashTask = null;
        }
        if (this.stashManager != null){
            this.stashManager.saveAll();
        }
        this.playerDataManager.disable();
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
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
    public StashManager getStashManager() {
        return this.stashManager;
    }

    @Override
    public PlayerDataManager getPlayerDataManager(){
        return this.playerDataManager;
    }


}
