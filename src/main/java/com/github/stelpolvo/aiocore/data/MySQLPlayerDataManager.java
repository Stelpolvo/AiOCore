package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.storage.StashStorage;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

@SuppressWarnings("all")
public class MySQLPlayerDataManager extends SQLPlayerDataManager {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS aio (
            uuid CHAR(36) PRIMARY KEY,
            username VARCHAR(16) NOT NULL,
            config_data TEXT NOT NULL,
            INDEX idx_aio_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

    private static final String UPSERT_PLAYER = """
            INSERT INTO aio (uuid, username, config_data)
            VALUES (?, ?, '{}')
            ON DUPLICATE KEY UPDATE username = VALUES(username)
            """;

    private static final String UPDATE_AIO = """
            UPDATE aio SET config_data = ? WHERE uuid = ?
            """;

    private static final String SELECT_BY_UUID = "SELECT * FROM aio WHERE uuid = ?";
    private static final String SELECT_BY_NAME = "SELECT * FROM aio WHERE username = ?";

    private static final String CREATE_STASH_TABLE = """
            CREATE TABLE IF NOT EXISTS aio_stash (
            uuid CHAR(36) NOT NULL,
            page INT NOT NULL,
            data TEXT,
            PRIMARY KEY (uuid, page)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

    private static final String UPSERT_STASH = """
            INSERT INTO aio_stash (uuid, page, data) VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE data = VALUES(data)
            """;

    private static final String SELECT_STASH = "SELECT data FROM aio_stash WHERE uuid = ? AND page = ?";

    private static final StashStorage.SqlConfig STASH_SQL = new StashStorage.SqlConfig() {
        @Override
        public String createTableSql() {
            return CREATE_STASH_TABLE;
        }

        @Override
        public String upsertSql() {
            return UPSERT_STASH;
        }

        @Override
        public String selectSql() {
            return SELECT_STASH;
        }
    };

    public MySQLPlayerDataManager(ConfigurationSection config, Logger logger, Messenger messenger) {
        super(config, logger, messenger, "com.mysql.cj.jdbc.Driver");
        initialize();
    }

    @Override protected String createTableSQL()   { return CREATE_TABLE; }
    @Override protected String upsertPlayerSQL()  { return UPSERT_PLAYER; }
    @Override protected String updateSQL()        { return UPDATE_AIO; }
    @Override protected String selectByUuidSQL()  { return SELECT_BY_UUID; }
    @Override protected String selectByNameSQL()  { return SELECT_BY_NAME; }
    @Override protected StashStorage.SqlConfig getStashSql() { return STASH_SQL; }
}