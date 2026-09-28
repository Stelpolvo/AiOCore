package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.AiO;
import com.github.stelpolvo.aiocore.api.Messenger;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class SQLitePlayerDataManager extends SQLPlayerDataManager {

    public static final String CREATE_PLAYER_TABLE = """
            CREATE TABLE IF NOT EXISTS aio (
            uuid VARCHAR(36) PRIMARY KEY,
            username VARCHAR(16) NOT NULL,
            json_data TEXT DEFAULT '{}'
            );
            """;

    public static final String INSERT_PLAYER = """
            INSERT OR IGNORE INTO aio (uuid, username, json_data)
            VALUES (?, ?, '{}')
            """;

    public static final String UPDATE_AIO_BY_UUID = """
            UPDATE aio
            SET json_data = ?
            WHERE uuid = ?
            """;

    public SQLitePlayerDataManager(AiO aio, ConfigurationSection config,
                                   Logger logger, Messenger messenger) {
        super(config, logger, messenger, "org.sqlite.JDBC");

        String url = config.getString("url", "aio.db");
        if (url.startsWith("jdbc:sqlite:")) {
            url = url.substring("jdbc:sqlite:".length());
        }
        File file = new File(aio.getJavaPlugin().getDataFolder(), url);
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        hikariConfig.setMaximumPoolSize(1);

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