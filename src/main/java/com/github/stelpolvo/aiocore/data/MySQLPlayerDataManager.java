package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.github.stelpolvo.aiocore.api.data.EconomyData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
import com.github.stelpolvo.aiocore.model.chat.ChatDataImpl;
import com.github.stelpolvo.aiocore.model.economy.EconomyDataImpl;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class MySQLPlayerDataManager extends SQLPlayerDataManager {

    public static final String CREATE_PLAYER_TABLE = """
            CREATE TABLE IF NOT EXISTS aio (
            uuid CHAR(36) PRIMARY KEY,
            name VARCHAR(16) NOT NULL,
            economy_data TEXT NOT NULL,
            chat_data TEXT NOT NULL,
            INDEX idx_aio_name (name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

    public static final String SELECT_PLAYER_BY_NAME = "SELECT * FROM aio WHERE name = ?;";
    public static final String SELECT_PLAYER_BY_UUID = "SELECT * FROM aio WHERE uuid = ?;";

    public static final String INSERT_PLAYER = """
            INSERT IGNORE INTO aio (uuid, name, economy_data, chat_data)
            VALUES (?, ?, '{}', '{}')
            """;

    public static final String UPDATE_AIO_BY_UUID = """
            UPDATE aio
            SET economy_data = ?, chat_data = ?
            WHERE uuid = ?
            """;

    private final Gson gson = new Gson();
    private final Map<String, UUID> playerRecordMap = new HashMap<>();

    public MySQLPlayerDataManager(ConfigurationSection config, Logger logger, Messenger messenger) {
        super(config, logger, messenger, "com.mysql.cj.jdbc.Driver");

        Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(p -> p.getName() != null)
                .forEach(p -> playerRecordMap.put(p.getName(), p.getUniqueId()));

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_PLAYER_TABLE)) {
            ps.execute();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize aio table", e);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        playerRecordMap.put(name, uuid);

        if (playerDataMap.containsKey(uuid)) {
            return;
        }

        PlayerData loaded = loadFromDatabase(uuid);
        if (loaded != null) {
            playerDataMap.put(uuid, loaded);
            return;
        }

        PlayerData fresh = new MySQLPlayerData(uuid);
        fresh.setEconomyData(new EconomyDataImpl(new HashMap<>()));
        fresh.setChatData(new ChatDataImpl(
                ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY
        ));
        fresh.getEconomyData().setInit(true);
        fresh.getChatData().setInit(true);
        playerDataMap.put(uuid, fresh);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_PLAYER)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to insert player " + uuid, e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = playerDataMap.get(uuid);

        if (data != null) {
            save(data);
        } else {
            logger.warning("No data to save for disconnecting player " + uuid);
        }

        playerDataMap.remove(uuid);
    }

    private PlayerData packet(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        PlayerData data = new MySQLPlayerData(uuid);

        Map<String, Double> economyMap = new HashMap<>();
        String ecoJson = rs.getString("economy_data");
        if (ecoJson != null && !ecoJson.isBlank()) {
            try {
                JsonObject ecoObj = gson.fromJson(ecoJson, JsonObject.class);
                if (ecoObj != null) {
                    for (Map.Entry<String, JsonElement> entry : ecoObj.entrySet()) {
                        try {
                            economyMap.put(entry.getKey(), entry.getValue().getAsDouble());
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warning("Corrupted economy_data for player " + uuid);
            }
        }
        data.setEconomyData(new EconomyDataImpl(economyMap));

        Map<String, String> chatMap = new HashMap<>();
        String chatJson = rs.getString("chat_data");
        if (chatJson != null && !chatJson.isBlank()) {
            try {
                JsonObject chatObj = gson.fromJson(chatJson, JsonObject.class);
                if (chatObj != null) {
                    for (Map.Entry<String, JsonElement> entry : chatObj.entrySet()) {
                        try {
                            chatMap.put(entry.getKey(), entry.getValue().getAsString());
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warning("Corrupted chat_data for player " + uuid);
            }
        }
        data.setChatData(new ChatDataImpl(
                chatMap.getOrDefault("name", ChatData.DEFAULT_KEY),
                chatMap.getOrDefault("message", ChatData.DEFAULT_KEY),
                chatMap.getOrDefault("chat", ChatData.DEFAULT_KEY),
                chatMap.getOrDefault("sound", ChatData.DEFAULT_KEY),
                chatMap.getOrDefault("channel", ChatData.DEFAULT_KEY)
        ));

        String name = rs.getString("name");
        if (name != null) {
            playerRecordMap.put(name, uuid);
        }

        return data;
    }

    private PlayerData loadFromDatabase(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER_BY_UUID)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return packet(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load player " + uuid, e);
        }
        return null;
    }

    @Override
    public PlayerData getByName(String playerName) {
        UUID uuid = playerRecordMap.get(playerName);
        if (uuid != null) {
            return getByUUID(uuid);
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER_BY_NAME)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return packet(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load player by name " + playerName, e);
        }
        return null;
    }

    @Override
    public PlayerData getByUUID(UUID uuid) {
        PlayerData cached = playerDataMap.get(uuid);
        if (cached != null) {
            return cached;
        }
        return loadFromDatabase(uuid);
    }

    @Override
    public Map<UUID, PlayerData> getPlayerData() {
        return playerDataMap;
    }

    @Override
    public boolean save(PlayerData data) {
        if (data == null) {
            return false;
        }

        EconomyData economyData = data.getEconomyData();
        ChatData chatData = data.getChatData();

        if (economyData == null || chatData == null) {
            logger.warning("Player data has null components: " + data.getUUID());
            return false;
        }

        boolean ecoDirty = !economyData.isCurrent() || economyData.isInit();
        boolean chatDirty = !chatData.isCurrent() || chatData.isInit();
        if (!ecoDirty && !chatDirty) {
            return true;
        }

        UUID uuid = data.getUUID();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_AIO_BY_UUID)) {

            try {
                JsonObject ecoObj = new JsonObject();
                economyData.get().forEach(ecoObj::addProperty);

                JsonObject chatObj = new JsonObject();
                chatObj.addProperty("name", chatData.getNameStyle());
                chatObj.addProperty("message", chatData.getMessageStyle());
                chatObj.addProperty("chat", chatData.getChatStyle());
                chatObj.addProperty("sound", chatData.getSoundStyle());
                chatObj.addProperty("channel", chatData.getChannel());

                ps.setString(1, ecoObj.toString());
                ps.setString(2, chatObj.toString());
                ps.setString(3, uuid.toString());

                int affected = ps.executeUpdate();
                if (affected >= 1) {
                    conn.commit();
                    return true;
                }

                conn.rollback();
                logger.warning("No row to update for player " + uuid);
                return false;

            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            }

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to save data for player " + uuid, ex);
            return false;
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Unexpected error while saving data for player " + uuid, ex);
            return false;
        }
    }

    public void saveAll() {
        if (playerDataMap.isEmpty()) {
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_AIO_BY_UUID)) {

            try {
                for (Map.Entry<UUID, PlayerData> e : playerDataMap.entrySet()) {
                    PlayerData pd = e.getValue();
                    if (pd == null) {
                        continue;
                    }

                    EconomyData eco = pd.getEconomyData();
                    ChatData chat = pd.getChatData();
                    if (eco == null || chat == null) {
                        continue;
                    }

                    boolean ecoDirty = !eco.isCurrent() || eco.isInit();
                    boolean chatDirty = !chat.isCurrent() || chat.isInit();
                    if (!ecoDirty && !chatDirty) {
                        continue;
                    }

                    JsonObject ecoObj = new JsonObject();
                    eco.get().forEach(ecoObj::addProperty);

                    JsonObject chatObj = new JsonObject();
                    chatObj.addProperty("name", chat.getNameStyle());
                    chatObj.addProperty("message", chat.getMessageStyle());
                    chatObj.addProperty("chat", chat.getChatStyle());
                    chatObj.addProperty("sound", chat.getSoundStyle());
                    chatObj.addProperty("channel", chat.getChannel());

                    ps.setString(1, ecoObj.toString());
                    ps.setString(2, chatObj.toString());
                    ps.setString(3, e.getKey().toString());
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();

            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            }

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to save player data in batch", ex);
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Unexpected error while saving player data", ex);
        }
    }

    public void disable() {
        saveAll();
        dataSource.close();
    }

    public static class MySQLPlayerData extends AbstractPlayerData {

        public MySQLPlayerData(UUID uuid) {
            super(uuid);
        }

        @Override
        public EconomyData getEconomyData() {
            return economyData;
        }

        @Override
        public void setEconomyData(EconomyData data) {
            this.economyData = data;
        }

        @Override
        public ChatData getChatData() {
            return chatData;
        }

        @Override
        public void setChatData(ChatData data) {
            this.chatData = data;
        }
    }
}