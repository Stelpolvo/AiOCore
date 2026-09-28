package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

@SuppressWarnings("all")
public class MySQLPlayerDataManager extends SQLPlayerDataManager {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS aio (
            uuid CHAR(36) PRIMARY KEY,
            username VARCHAR(16) NOT NULL,
            json_data TEXT NOT NULL,
            INDEX idx_aio_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

    private static final String UPSERT_PLAYER = """
            INSERT INTO aio (uuid, username, json_data)
            VALUES (?, ?, '{}')
            ON DUPLICATE KEY UPDATE username = VALUES(username)
            """;

    private static final String UPDATE_AIO = """
            UPDATE aio SET json_data = ? WHERE uuid = ?
            """;

    private static final String SELECT_BY_UUID = "SELECT * FROM aio WHERE uuid = ?";
    private static final String SELECT_BY_NAME = "SELECT * FROM aio WHERE username = ?";

    public MySQLPlayerDataManager(ConfigurationSection config, Logger logger, Messenger messenger) {
        super(config, logger, messenger, "com.mysql.cj.jdbc.Driver");
        initialize();
    }

    @Override protected String createTableSQL()   { return CREATE_TABLE; }
    @Override protected String upsertPlayerSQL()  { return UPSERT_PLAYER; }
    @Override protected String updateSQL()        { return UPDATE_AIO; }
    @Override protected String selectByUuidSQL()  { return SELECT_BY_UUID; }
    @Override protected String selectByNameSQL()  { return SELECT_BY_NAME; }
}