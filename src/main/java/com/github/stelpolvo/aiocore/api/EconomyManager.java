package com.github.stelpolvo.aiocore.api;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

public interface EconomyManager extends Placeholder{

    void load(ConfigurationSection section, PlayerDataManager dataManager, Logger logger, Messenger messenger);

    void register(String currencyKey, AiOEconomy economy);

    boolean isEnabled();

    AiOEconomy getAiOEconomy(String currencyKey);

    AiOEconomy getVaultEconomy();

    List<String> getCurrencyList();

    default String getPlaceholderPrefix() {
        return EconomyManager.class.getSimpleName();
    }

    default String onPlaceholderRequest(final OfflinePlayer player, final @NotNull String[] params){
        return "NotImplemented";
    }

    interface AiOEconomy {

        Economy getEconomy();

        String getCurrencyKey();

        String getSymbol();

        String getFormat();

        void disable();

        boolean isTransferable();
    }
}
