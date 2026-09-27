package com.github.stelpolvo.aiocore.data;

import com.github.stelpolvo.aiocore.api.Messenger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

public abstract class SQLPlayerDataManager extends AbstractPlayerDataManager {

    protected HikariDataSource dataSource;
    protected HikariConfig hikariConfig = new HikariConfig();

    public SQLPlayerDataManager(ConfigurationSection config, Logger logger, Messenger messenger, String driverClassName) {
        super(logger, messenger);

        String url = config.getString("url");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Missing 'url' in database config");
        }


        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(config.getString("username", ""));
        hikariConfig.setPassword(config.getString("password", ""));
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMaximumPoolSize(config.getInt("pool-size", 10));
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(5000);
        hikariConfig.setIdleTimeout(60000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setPoolName("AiOCore-Pool");
        if (driverClassName != null && !driverClassName.isBlank()) {
            hikariConfig.setDriverClassName(driverClassName);
        }


    }

    protected void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}