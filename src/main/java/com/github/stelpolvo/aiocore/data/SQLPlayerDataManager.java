package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.github.stelpolvo.aiocore.api.data.EconomyData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
import com.github.stelpolvo.aiocore.api.storage.StashStorage;
import com.github.stelpolvo.aiocore.model.chat.ChatDataImpl;
import com.github.stelpolvo.aiocore.model.economy.EconomyDataImpl;
import com.github.stelpolvo.aiocore.model.stash.SqlStashStorage;
import com.github.stelpolvo.aiocore.model.stash.StashManagerImpl;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("all")
public abstract class SQLPlayerDataManager extends AbstractPlayerDataManager {

    protected final HikariConfig hikariConfig = new HikariConfig();
    protected HikariDataSource dataSource;
    protected final Gson gson = new Gson();
    protected final Map<String, UUID> playerRecordMap = new ConcurrentHashMap<>();

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
    protected abstract String upsertPlayerSQL();
    protected abstract String updateSQL();
    protected abstract String selectByUuidSQL();
    protected abstract String selectByNameSQL();

    /**
     * 仓库表（aio_stash）的方言语句。
     */
    protected abstract StashStorage.SqlConfig getStashSql();

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * 组装仓库存储，供 StashStorage.create 调用。
     */
    public StashStorage createStashStorage() {
        return new SqlStashStorage(dataSource, getStashSql(), logger);
    }

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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(upsertPlayerSQL())) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to upsert player row " + uuid, e);
        }

        PlayerData fresh = createPlayerData(uuid);
        fresh.setEconomyData(new EconomyDataImpl(new HashMap<>()));
        fresh.setChatData(new ChatDataImpl(
                ChatData.DEFAULT_KEY, ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY, ChatData.DEFAULT_KEY, ChatData.DEFAULT_KEY));
        fresh.getEconomyData().setInit(true);
        fresh.getChatData().setInit(true);
        bindStash(fresh);
        playerDataMap.put(uuid, fresh);
    }

    /**
     * 仓库数据由 StashManager 统一持有并按需载入，这里只把实例挂到玩家数据上。
     */
    private void bindStash(PlayerData data) {
        StashManagerImpl stash = StashManagerImpl.current();
        if (stash != null) {
            data.setStashData(stash.getStashData(data.getUUID()));
        }
    }

    protected PlayerData packet(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        PlayerData data = createPlayerData(uuid);

        JsonObject root = null;
        String json = rs.getString("config_data");
        if (json != null && !json.isBlank()) {
            try {
                root = gson.fromJson(json, JsonObject.class);
            } catch (Exception ex) {
                logger.warning("Corrupted config_data for player " + uuid);
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
             PreparedStatement ps = conn.prepareStatement(selectByUuidSQL())) {
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
             PreparedStatement ps = conn.prepareStatement(selectByNameSQL())) {
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
        if (data == null) return false;

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

                // 行不存在：说明 upsert 失败过，补一次
                conn.rollback();
                logger.warning("No row to update for player " + uuid + ", retrying upsert");

                try (PreparedStatement upsert = conn.prepareStatement(upsertPlayerSQL())) {
                    upsert.setString(1, uuid.toString());
                    upsert.setString(2, playerRecordMap.entrySet().stream()
                            .filter(e -> e.getValue().equals(uuid))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse("unknown"));
                    upsert.executeUpdate();

                    ps.setString(1, buildJson(economyData, chatData));
                    ps.setString(2, uuid.toString());
                    int retried = ps.executeUpdate();
                    conn.commit();

                    if (retried >= 1) {
                        economyData.setInit(false);
                        economyData.setCurrent(true);
                        chatData.setInit(false);
                        chatData.setCurrent(true);
                        return true;
                    }
                }
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
        int success = 0;
        int failed = 0;

        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            try {
                if (save(entry.getValue())) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                logger.log(Level.SEVERE, "Failed to save player " + entry.getKey()
                        + " during saveAll", e);
            }
        }

        if (success > 0) {
            messenger.send(Bukkit.getConsoleSender(), Messenger.SUCCESS_SAVE_DATA,
                    "time", (System.nanoTime() - start) / 1_000_000);
        }
        if (failed > 0) {
            logger.warning("saveAll completed with " + failed
                    + " failures out of " + playerDataMap.size());
        }
    }

    public void disable() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static final long HEALTH_CHECK_INTERVAL_MS = 5_000;
    private volatile long lastCheckAt = 0;
    private volatile boolean lastResult = true;

    @Override
    public boolean isEnabled() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastCheckAt < HEALTH_CHECK_INTERVAL_MS) {
            return lastResult;
        }

        synchronized (this) {
            if (System.currentTimeMillis() - lastCheckAt < HEALTH_CHECK_INTERVAL_MS) {
                return lastResult;
            }

            boolean result;
            try (Connection c = dataSource.getConnection()) {
                result = c.isValid(2);
            } catch (SQLException e) {
                result = false;
                logger.log(Level.WARNING, "Database health check failed", e);
            } catch (RuntimeException e) {
                result = false;
                logger.log(Level.SEVERE, "Unexpected error during health check", e);
            }

            lastCheckAt = System.currentTimeMillis();
            lastResult = result;
            return result;
        }
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