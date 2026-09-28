package com.github.stelpolvo.aiocore.api;

import com.github.stelpolvo.aiocore.api.data.StashData;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public interface StashManager extends Listener {

    void open(Player player, int page);

    void open(Player viewer, UUID owner, String ownerName, int page);

    StashData getStashData(UUID uuid);

    void save(UUID uuid, int page);

    void save(UUID uuid);

    void saveAll();

    void evict(UUID uuid);

    int getMaxPages();

    boolean isEnabled();

    void onInventoryClose(InventoryCloseEvent event);

    interface StashHolder extends InventoryHolder {
        void setInventory(Inventory inventory);

        int getPage();

        UUID getOwner();
    }
}
