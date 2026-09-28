package com.github.stelpolvo.aiocore.model.stash;

import com.github.stelpolvo.aiocore.api.StashManager;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class StashHolderImpl implements StashManager.StashHolder {

    private final UUID owner;
    private final int page;
    private Inventory inventory;

    public StashHolderImpl(UUID owner, int page) {
        this.owner = owner;
        this.page = page;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Inventory not initialized");
        }
        return inventory;
    }
}
