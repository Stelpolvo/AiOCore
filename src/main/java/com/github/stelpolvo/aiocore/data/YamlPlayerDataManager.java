package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.github.stelpolvo.aiocore.api.data.EconomyData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
import com.github.stelpolvo.aiocore.model.chat.ChatDataImpl;
import com.github.stelpolvo.aiocore.model.economy.EconomyDataImpl;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class YamlPlayerDataManager extends AbstractPlayerDataManager {

    private final File dataFolder;

    public YamlPlayerDataManager(File dataFolder, Logger logger, Messenger messenger) {
        super(logger, messenger);
        this.dataFolder = dataFolder;

        if (dataFolder == null) {
            throw new IllegalStateException("Data folder is null");
        }
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.log(Level.SEVERE, "Failed to create data folder: " + dataFolder.getAbsolutePath());
        }

        Bukkit.getOnlinePlayers().forEach(p -> {
            try {
                getByUUID(p.getUniqueId());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load data for online player " + p.getUniqueId(), e);
            }
        });
    }

    @Override
    public PlayerData getByName(String playerName) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            if (player != null && player.hasPlayedBefore() && player.getUniqueId() != null) {
                return getByUUID(player.getUniqueId());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get player by name: " + playerName, e);
        }
        return null;
    }

    @Override
    public PlayerData getByUUID(UUID uuid) {
        if (uuid == null) {
            logger.warning("getByUUID called with null UUID");
            return null;
        }
        try {
            return playerDataMap.computeIfAbsent(uuid, this::loadFromDisk);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load data for player " + uuid, e);
            return null;
        }
    }

    private YamlPlayerData loadFromDisk(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        if (file.exists()) {
            try {
                yaml.load(file);
            } catch (IOException e) {
                logger.log(Level.SEVERE,
                        "Failed to read YAML file for player " + uuid
                                + " at " + file.getAbsolutePath() + ", using empty data", e);
                yaml = new YamlConfiguration();
            } catch (InvalidConfigurationException e) {
                logger.log(Level.SEVERE,
                        "Corrupted YAML file for player " + uuid
                                + " at " + file.getAbsolutePath() + ", using empty data", e);
                yaml = new YamlConfiguration();
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE,
                        "Unexpected error while loading YAML for player " + uuid
                                + ", using empty data", e);
                yaml = new YamlConfiguration();
            }
        }

        YamlPlayerData data = new YamlPlayerData(uuid, yaml, logger);

        if (!file.exists()) {
            data.getEconomyData().setInit(true);
            data.getChatData().setInit(true);
        }
        return data;
    }

    @Override
    public Map<UUID, PlayerData> getPlayerData() {
        return playerDataMap;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            getByUUID(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load data on join for player "
                    + event.getPlayer().getUniqueId(), e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = playerDataMap.remove(uuid);

        if (data == null) {
            return;
        }
        try {
            save(data);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save data on quit for player " + uuid, e);
        }
    }

    @Override
    public boolean save(PlayerData playerData) {
        if (playerData == null) {
            logger.log(Level.SEVERE, "save called with null PlayerData");
            return false;
        }
        if (!(playerData instanceof YamlPlayerData yamlPlayerData)) {
            logger.log(Level.SEVERE, "Player data is not a YamlPlayerData: "
                    + playerData.getClass().getName());
            return false;
        }

        EconomyData economyData = playerData.getEconomyData();
        ChatData chatData = playerData.getChatData();
        if (economyData == null || chatData == null) {
            logger.warning("Player data has null components for player " + yamlPlayerData.uuid);
            return false;
        }

        boolean ecoDirty = !economyData.isCurrent() || economyData.isInit();
        boolean chatDirty = !chatData.isCurrent() || chatData.isInit();
        if (!ecoDirty && !chatDirty) {
            return true;
        }

        File file = new File(dataFolder, yamlPlayerData.uuid + ".yml");

        try {
            YamlConfiguration yaml = new YamlConfiguration();
            if (file.exists()) {
                try {
                    yaml.load(file);
                } catch (IOException e) {
                    logger.log(Level.WARNING,
                            "Failed to reload existing YAML for player " + yamlPlayerData.uuid
                                    + ", overwriting with current data", e);
                    yaml = new YamlConfiguration();
                } catch (InvalidConfigurationException e) {
                    logger.log(Level.WARNING,
                            "Corrupted existing YAML for player " + yamlPlayerData.uuid
                                    + ", overwriting with current data", e);
                    yaml = new YamlConfiguration();
                }
            }

            if (ecoDirty) {
                yaml.set("economy", economyData.get());
            }
            if (chatDirty) {
                yaml.set("chat.name", chatData.getNameStyle());
                yaml.set("chat.message", chatData.getMessageStyle());
                yaml.set("chat.chat", chatData.getChatStyle());
                yaml.set("chat.sound", chatData.getSoundStyle());
                yaml.set("chat.channel", chatData.getChannel());
            }

            yaml.save(file);

            economyData.setInit(false);
            economyData.setCurrent(true);
            chatData.setInit(false);
            chatData.setCurrent(true);
            return true;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write YAML file for player "
                    + yamlPlayerData.uuid + " at " + file.getAbsolutePath(), e);
            return false;
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Unexpected error while saving data for player "
                    + yamlPlayerData.uuid, e);
            return false;
        }
    }

    @Override
    public void saveAll() {
        if (playerDataMap.isEmpty()) {
            return;
        }

        long start = System.nanoTime();
        boolean anySaved = false;

        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            try {
                if (save(entry.getValue())) {
                    anySaved = true;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to save data for player "
                        + entry.getKey() + " during batch save", e);
            }
        }

        if (anySaved) {
            try {
                messenger.send(Bukkit.getConsoleSender(), Messenger.SUCCESS_SAVE_DATA,
                        "time", (System.nanoTime() - start) / 1_000_000);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to send save notification", e);
            }
        }
    }

    @Override
    public void disable() {
        try {
            saveAll();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save all data on disable", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return dataFolder != null && dataFolder.exists();
    }

    public static class YamlPlayerData extends AbstractPlayerData {

        public YamlPlayerData(UUID uuid, YamlConfiguration yaml, Logger logger) {
            super(uuid);

            Map<String, Double> economyMap = new HashMap<>();
            try {
                ConfigurationSection ecoSection = yaml.getConfigurationSection("economy");
                if (ecoSection != null) {
                    for (String key : ecoSection.getKeys(false)) {
                        try {
                            economyMap.put(key, ecoSection.getDouble(key));
                        } catch (Exception e) {
                            logger.log(Level.WARNING,
                                    "Skipping invalid economy entry '" + key
                                            + "' for player " + uuid, e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "Failed to read economy section for player " + uuid
                                + ", using empty data", e);
            }
            this.economyData = new EconomyDataImpl(economyMap);

            String nameStyle = ChatData.DEFAULT_KEY;
            String messageStyle = ChatData.DEFAULT_KEY;
            String chatStyle = ChatData.DEFAULT_KEY;
            String soundStyle = ChatData.DEFAULT_KEY;
            String channel = ChatData.DEFAULT_KEY;

            try {
                ConfigurationSection chatSection = yaml.getConfigurationSection("chat");
                if (chatSection != null) {
                    nameStyle = chatSection.getString("name", ChatData.DEFAULT_KEY);
                    messageStyle = chatSection.getString("message", ChatData.DEFAULT_KEY);
                    chatStyle = chatSection.getString("chat", ChatData.DEFAULT_KEY);
                    soundStyle = chatSection.getString("sound", ChatData.DEFAULT_KEY);
                    channel = chatSection.getString("channel", ChatData.DEFAULT_KEY);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "Failed to read chat section for player " + uuid
                                + ", using default styles", e);
            }

            this.chatData = new ChatDataImpl(nameStyle, messageStyle, chatStyle, soundStyle, channel);
        }

        @Override
        public EconomyData getEconomyData() {
            return this.economyData;
        }

        @Override
        public void setEconomyData(EconomyData data) {
            this.economyData = data;
        }

        @Override
        public ChatData getChatData() {
            return this.chatData;
        }

        @Override
        public void setChatData(ChatData data) {
            this.chatData = data;
        }
    }
}