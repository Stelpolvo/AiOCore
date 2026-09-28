package com.github.stelpolvo.aiocore.model.stash;

import com.github.stelpolvo.aiocore.api.storage.StashStorage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlStashStorage implements StashStorage {

    private final Gson gson = new Gson();
    private final HikariDataSource dataSource;
    private final StashStorage.SqlConfig sql;
    private final Logger logger;
    private final Map<String, JsonObject> pending = new ConcurrentHashMap<>();

    public SqlStashStorage(HikariDataSource dataSource, StashStorage.SqlConfig sql, Logger logger) {
        this.dataSource = dataSource;
        this.sql = sql;
        this.logger = logger;
        createTable();
    }

    private void createTable() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.createTableSql())) {
            ps.execute();
            conn.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create stash table", e);
        }
    }

    @Override
    public JsonObject load(UUID uuid, int page) {
        JsonObject cached = pending.get(key(uuid, page));
        if (cached != null) {
            return cached;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.selectSql())) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, page);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String json = rs.getString("data");
                if (json == null || json.isBlank()) {
                    return null;
                }
                return gson.fromJson(json, JsonObject.class);
            }
        } catch (SQLException | RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to load stash page " + page
                    + " for " + uuid, e);
            return null;
        }
    }

    @Override
    public boolean save(UUID uuid, int page, JsonObject root) {
        pending.put(key(uuid, page), root);
        return true;
    }

    @Override
    public void flush() {
        for (Map.Entry<String, JsonObject> entry : pending.entrySet()) {
            String key = entry.getKey();
            int split = key.lastIndexOf('|');
            if (split <= 0) {
                continue;
            }
            UUID uuid;
            int page;
            try {
                uuid = UUID.fromString(key.substring(0, split));
                page = Integer.parseInt(key.substring(split + 1));
            } catch (RuntimeException e) {
                pending.remove(key);
                continue;
            }
            if (write(uuid, page, entry.getValue())) {
                pending.remove(key, entry.getValue());
            }
        }
    }

    private boolean write(UUID uuid, int page, JsonObject root) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.upsertSql())) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, page);
            ps.setString(3, gson.toJson(root));
            ps.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException | RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to save stash page " + page
                    + " for " + uuid, e);
            return false;
        }
    }

    private static String key(UUID uuid, int page) {
        return uuid + "|" + page;
    }
}
