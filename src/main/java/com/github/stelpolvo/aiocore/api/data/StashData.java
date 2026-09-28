package com.github.stelpolvo.aiocore.api.data;

import com.google.gson.JsonObject;
import org.bukkit.inventory.Inventory;

import java.util.Set;
import java.util.UUID;

public interface StashData {

    /**
     * 页索引，0 为第一页。
     */
    int FIRST_PAGE = 0;

    /**
     * 取得指定页的背包，不存在时懒创建。
     */
    Inventory get(int page);

    boolean isPageLoaded(int page);

    Set<Integer> getLoadedPages();

    JsonObject toJson(int page);

    void loadFromJson(int page, JsonObject root);

    /**
     * 标记某一页已被改动。
     */
    void markModified(int page);

    boolean isModified(int page);

    void markSaved(int page);

    boolean isDirty();

    UUID getOwner();
}
