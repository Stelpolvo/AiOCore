package com.github.stelpolvo.aiocore.api;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.logging.Logger;

public interface EconomyManager {

    void load(ConfigurationSection section, PlayerDataManager dataManager, Logger logger);

    void register(String currencyKey, AiOEconomy economy);

    AiOEconomy getAiOEconomy(String currencyKey);

    AiOEconomy getVaultEconomy();

    List<String> getCurrencyList();

    interface AiOEconomy {

        Economy getEconomy();

        void disable();

        boolean isTransferable();
    }
}
