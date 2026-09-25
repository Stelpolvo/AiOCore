package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.EconomyData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
import com.github.stelpolvo.aiocore.utils.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YamlPlayerDataManager implements PlayerDataManager{
    private final File dataFolder;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Logger logger;
    private final Messenger messenger;

    public YamlPlayerDataManager(File dataFolder, Logger logger, Messenger messenger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.messenger = messenger;
    }

    @Override
    public PlayerData getByName(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player.hasPlayedBefore()) {
            return getByUUID(player.getUniqueId());
        }
        return null;
    }

    @Override
    public PlayerData getByUUID(UUID uuid) {
        PlayerData playerData = playerDataMap.get(uuid);
        if (playerData == null) {
            File dataFile = FileUtil.createIfNotExists(new File(dataFolder, uuid.toString() + ".yml"), false);
            YamlConfiguration dataYaml = YamlConfiguration.loadConfiguration(dataFile);
            playerData = new YamlPlayerData(dataYaml, uuid);
            playerDataMap.put(uuid, playerData);
        }
        return playerData;
    }

    @Override
    public Map<UUID, PlayerData> getPlayerData(){
        return playerDataMap;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getByUUID(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        save(getByUUID(event.getPlayer().getUniqueId()));
    }

    @Override
    public void save(PlayerData playerData) {
        if (playerData == null) {
            logger.log(Level.SEVERE, "Player data is null!");
            return;
        }
        if (playerData instanceof YamlPlayerData yamlPlayerData) {
            File file = new File(dataFolder, yamlPlayerData.uuid.toString() + ".yml");
            try {
                YamlConfiguration dataYaml = new YamlConfiguration();
                dataYaml.set("economy", playerData.getEconomyData().get());
                dataYaml.save(file);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to save player data", e);
            }
        }else {
            logger.log(Level.SEVERE, "Player data is not a YamlPlayerData!");
        }
    }

    @Override
    public void saveAll(){
        playerDataMap.values().forEach(this::save);
        this.messenger.send(Bukkit.getConsoleSender(), Messenger.SUCCESS_SAVE_DATA);
    }

    public static class YamlPlayerData implements PlayerData {
        protected final UUID uuid;
        protected EconomyData economyData;
        public YamlPlayerData(YamlConfiguration data, UUID uuid) {
            this.uuid = uuid;
            ConfigurationSection ecoSection = data.getConfigurationSection("economy");
            Map<String, Double> economyMap = new HashMap<>();
            if (ecoSection != null) {
                ecoSection.getKeys(false).forEach(k -> economyMap.put(k, ecoSection.getDouble(k)));
            }
            this.economyData = new EconomyData(economyMap);
        }

        public EconomyData getEconomyData() {
            return this.economyData;
        }
    }
}
