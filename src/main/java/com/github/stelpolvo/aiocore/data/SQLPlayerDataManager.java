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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
public abstract class SQLPlayerDataManager extends AbstractPlayerDataManager {

    protected final HikariConfig hikariConfig = new HikariConfig();
    protected HikariDataSource dataSource;
    protected final Gson gson = new Gson();
    protected final Map<String, UUID> playerRecordMap = new HashMap<>();

    public SQLPlayerDataManager(ConfigurationSection config, Logger logger,
                                Messenger messenger, String driverClassName) {
        super(logger, messenger);

        String url = config.getString("url");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Missing 'url' in database config");
        }

        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(config.getString("username", ""));
        hikariConfig.setPassword(config.getString("password", ""));
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMaximumPoolSize(config.getInt("pool-size", 10));
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(5_000);
        hikariConfig.setIdleTimeout(60_000);
        hikariConfig.setMaxLifetime(1_800_000);
        hikariConfig.setPoolName("AiOCore-Pool");
    }

    protected abstract String createTableSQL();
    protected abstract String insertPlayerSQL();
    protected abstract String updateSQL();

    protected PlayerData createPlayerData(UUID uuid) {
        return new SimplePlayerData(uuid);
    }

    protected void initialize() {
        this.dataSource = new HikariDataSource(hikariConfig);

        Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(p -> p.getName() != null)
                .forEach(p -> playerRecordMap.put(p.getName(), p.getUniqueId()));

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(createTableSQL())) {
            ps.execute();
            conn.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize table", e);
        }

        Bukkit.getOnlinePlayers().forEach(p -> onJoin(p.getUniqueId(), p.getName()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        onJoin(player.getUniqueId(), player.getName());
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

    protected void onJoin(UUID uuid, String name) {
        playerRecordMap.put(name, uuid);

        if (playerDataMap.containsKey(uuid)) {
            return;
        }

        PlayerData loaded = loadFromDatabase(uuid);
        if (loaded != null) {
            playerDataMap.put(uuid, loaded);
            return;
        }

        PlayerData fresh = createPlayerData(uuid);
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
             PreparedStatement ps = conn.prepareStatement(insertPlayerSQL())) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to insert player " + uuid, e);
        }
    }

    protected PlayerData packet(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        PlayerData data = createPlayerData(uuid);

        JsonObject root = null;
        String json = rs.getString("json_data");
        if (json != null && !json.isBlank()) {
            try {
                root = gson.fromJson(json, JsonObject.class);
            } catch (Exception ex) {
                logger.warning("Corrupted json_data for player " + uuid);
            }
        }

        Map<String, Double> economyMap = new HashMap<>();
        if (root != null && root.has("economy") && root.get("economy").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("economy").entrySet()) {
                try {
                    economyMap.put(entry.getKey(), entry.getValue().getAsDouble());
                } catch (Exception ignored) {
                }
            }
        }
        data.setEconomyData(new EconomyDataImpl(economyMap));

        Map<String, String> chatMap = new HashMap<>();
        if (root != null && root.has("chat") && root.get("chat").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("chat").entrySet()) {
                try {
                    chatMap.put(entry.getKey(), entry.getValue().getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        data.setChatData(new ChatDataImpl(
                chatMap.getOrDefault("name", ChatData.DEFAULT_KEY),
                chatMap.getOrDefault("message", ChatData.DEFAULT_KEY),
                chatMap.getOrDefault("chat", ChatData.DEFAULT_KEY),
                chatMap.getOrDefault("sound", ChatData.DEFAULT_KEY),
                chatMap.getOrDefault("channel", ChatData.DEFAULT_KEY)
        ));

        String name = rs.getString("username");
        if (name != null) {
            playerRecordMap.put(name, uuid);
        }
        return data;
    }

    protected PlayerData loadFromDatabase(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM aio WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return packet(rs);
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
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM aio WHERE username = ?")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return packet(rs);
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

    protected String buildJson(EconomyData economyData, ChatData chatData) {
        JsonObject root = new JsonObject();

        JsonObject ecoObj = new JsonObject();
        economyData.get().forEach(ecoObj::addProperty);
        root.add("economy", ecoObj);

        JsonObject chatObj = new JsonObject();
        chatObj.addProperty("name", chatData.getNameStyle());
        chatObj.addProperty("message", chatData.getMessageStyle());
        chatObj.addProperty("chat", chatData.getChatStyle());
        chatObj.addProperty("sound", chatData.getSoundStyle());
        chatObj.addProperty("channel", chatData.getChannel());
        root.add("chat", chatObj);

        return root.toString();
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
             PreparedStatement ps = conn.prepareStatement(updateSQL())) {

            try {
                ps.setString(1, buildJson(economyData, chatData));
                ps.setString(2, uuid.toString());

                int affected = ps.executeUpdate();
                if (affected >= 1) {
                    conn.commit();
                    economyData.setInit(false);
                    economyData.setCurrent(true);
                    chatData.setInit(false);
                    chatData.setCurrent(true);
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

        long start = System.nanoTime();
        boolean isSuccess = false;
        boolean hasDirty = false;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSQL())) {

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

                    hasDirty = true;
                    ps.setString(1, buildJson(eco, chat));
                    ps.setString(2, e.getKey().toString());
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();
                isSuccess = true;
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
        } finally {
            if (isSuccess && hasDirty) {
                playerDataMap.values().forEach(d -> {
                    d.getEconomyData().setCurrent(true);
                    d.getEconomyData().setInit(false);
                    d.getChatData().setCurrent(true);
                    d.getChatData().setInit(false);
                });
                messenger.send(Bukkit.getConsoleSender(), Messenger.SUCCESS_SAVE_DATA,
                        "time", (System.nanoTime() - start) / 1_000_000);
            }
        }
    }

    public void disable() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public boolean isEnabled() {
        return dataSource != null && dataSource.isRunning();
    }

    public static class SimplePlayerData extends AbstractPlayerData {

        public SimplePlayerData(UUID uuid) {
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