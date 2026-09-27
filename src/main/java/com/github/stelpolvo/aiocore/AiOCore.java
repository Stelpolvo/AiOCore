package com.github.stelpolvo.aiocore;

import com.github.stelpolvo.aiocore.api.*;
import com.github.stelpolvo.aiocore.command.MainCommand;
import com.github.stelpolvo.aiocore.data.MySQLPlayerDataManager;
import com.github.stelpolvo.aiocore.data.SQLitePlayerDataManager;
import com.github.stelpolvo.aiocore.data.YamlPlayerDataManager;
import com.github.stelpolvo.aiocore.model.chat.ChatManagerImpl;
import com.github.stelpolvo.aiocore.model.economy.EconomyManagerImpl;
import com.github.stelpolvo.aiocore.model.message.MessengerImpl;
import com.github.stelpolvo.aiocore.model.placeholder.AiOHook;
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
    private PlayerDataManager playerDataManager;
    private Messenger messenger;

    private BukkitTask task;
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
        task = new BukkitRunnable() {
            public void run() {

                saveData();
            }
        }.runTaskTimer(this, updateDuration, updateDuration);
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
