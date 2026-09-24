package com.github.stelpolvo.aiocore;

import com.github.stelpolvo.aiocore.api.AiO;
import com.github.stelpolvo.aiocore.api.EconomyManager;
import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.command.MainCommand;
import com.github.stelpolvo.aiocore.model.economy.EconomyManagerImpl;
import com.github.stelpolvo.aiocore.model.message.MessengerImpl;
import com.github.stelpolvo.aiocore.model.placeholder.AiOHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
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
        // economy
        this.economyManager = new EconomyManagerImpl();
        this.economyManager.load(config.getConfigurationSection("economy"), this.playerDataManager, getLogger());



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
    public PlayerDataManager playerDataManager(){
        return this.playerDataManager;
    }


}
