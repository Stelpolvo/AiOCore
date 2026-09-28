package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

@SuppressWarnings("all")
public class MySQLPlayerDataManager extends SQLPlayerDataManager {

    public static final String CREATE_PLAYER_TABLE = """
            CREATE TABLE IF NOT EXISTS aio (
            uuid CHAR(36) PRIMARY KEY,
            username VARCHAR(16) NOT NULL,
            json_data TEXT NOT NULL,
            INDEX idx_aio_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

    public static final String INSERT_PLAYER = """
            INSERT IGNORE INTO aio (uuid, username, json_data)
            VALUES (?, ?, '{}')
            """;

    public static final String UPDATE_AIO_BY_UUID = """
            UPDATE aio
            SET json_data = ?
            WHERE uuid = ?
            """;

    public MySQLPlayerDataManager(ConfigurationSection config, Logger logger, Messenger messenger) {
        super(config, logger, messenger, "com.mysql.cj.jdbc.Driver");
        initialize();
    }

    @Override
    protected String createTableSQL() {
        return CREATE_PLAYER_TABLE;
    }

    @Override
    protected String insertPlayerSQL() {
        return INSERT_PLAYER;
    }

    @Override
    protected String updateSQL() {
        return UPDATE_AIO_BY_UUID;
    }
}