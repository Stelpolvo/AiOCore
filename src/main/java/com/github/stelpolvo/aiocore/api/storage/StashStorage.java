package com.github.stelpolvo.aiocore.api.storage;

import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.data.AbstractPlayerDataManager;
import com.github.stelpolvo.aiocore.data.SQLPlayerDataManager;
import com.github.stelpolvo.aiocore.model.stash.JsonStashStorage;
import com.github.stelpolvo.aiocore.model.stash.SqlStashStorage;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 仓库落盘策略。
 * yml 数据源写入 &lt;数据目录&gt;/stash/&lt;uuid&gt;.json，
 * sql 数据源写入 aio_stash 表（一页一行，key 为页索引，0 起）。
 */
public interface StashStorage {

    /**
     * 读取一页，没有数据返回 null。
     */
    JsonObject load(UUID uuid, int page);

    boolean save(UUID uuid, int page, JsonObject root);

    /**
     * 批量落盘，由主线程定时任务调用。
     */
    void flush();

    static StashStorage create(PlayerDataManager manager, Logger logger) {
        if (manager instanceof SQLPlayerDataManager sql) {
            return sql.createStashStorage();
        }
        if (manager instanceof AbstractPlayerDataManager dataManager) {
            File folder = dataManager.getStashFolder();
            if (folder != null) {
                return new JsonStashStorage(folder, logger);
            }
        }
        logger.log(Level.WARNING, "Player data source "
                + (manager == null ? "null" : manager.getClass().getName())
                + " has no stash storage, stash data will not be persisted");
        return new StashStorage() {
            @Override
            public JsonObject load(UUID uuid, int page) {
                return null;
            }

            @Override
            public boolean save(UUID uuid, int page, JsonObject root) {
                return false;
            }

            @Override
            public void flush() {
            }
        };
    }

    /**
     * SQL 数据源下 aio_stash 表的方言差异，由 MySQL / SQLite 实现分别给出。
     */
    interface SqlConfig {
        String createTableSql();

        String upsertSql();

        String selectSql();
    }
}
