package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.AiO;
import com.github.stelpolvo.aiocore.api.Messenger;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class SQLitePlayerDataManager extends SQLPlayerDataManager {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS aio (
            uuid VARCHAR(36) PRIMARY KEY,
            username VARCHAR(16) NOT NULL,
            json_data TEXT DEFAULT '{}'
            );
            """;

    private static final String UPSERT_PLAYER = """
            INSERT OR IGNORE INTO aio (uuid, username, json_data)
            VALUES (?, ?, '{}')
            """;

    private static final String UPDATE_AIO = """
            UPDATE aio SET json_data = ? WHERE uuid = ?
            """;

    private static final String SELECT_BY_UUID = "SELECT * FROM aio WHERE uuid = ?";
    private static final String SELECT_BY_NAME = "SELECT * FROM aio WHERE username = ?";

    public SQLitePlayerDataManager(AiO aio, ConfigurationSection config,
                                   Logger logger, Messenger messenger) {
        super(config, logger, messenger, "org.sqlite.JDBC");

        hikariConfig.setMaximumPoolSize(1);

        String url = config.getString("url", "aio.db");
        if (url.startsWith("jdbc:sqlite:")) {
            url = url.substring("jdbc:sqlite:".length());
        }
        File file = new File(aio.getJavaPlugin().getDataFolder(), url);
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());

        initialize();
    }

    @Override protected String createTableSQL()   { return CREATE_TABLE; }
    @Override protected String upsertPlayerSQL()  { return UPSERT_PLAYER; }
    @Override protected String updateSQL()        { return UPDATE_AIO; }
    @Override protected String selectByUuidSQL()  { return SELECT_BY_UUID; }
    @Override protected String selectByNameSQL()  { return SELECT_BY_NAME; }
}