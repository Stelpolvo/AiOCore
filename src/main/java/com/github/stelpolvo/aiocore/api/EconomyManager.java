package com.github.stelpolvo.aiocore.api;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.logging.Logger;

public interface EconomyManager {

    void load(ConfigurationSection section, PlayerDataManager dataManager, Logger logger, Messenger messenger);

    void register(String currencyKey, AiOEconomy economy);

    boolean isEnabled();

    AiOEconomy getAiOEconomy(String currencyKey);

    AiOEconomy getVaultEconomy();

    List<String> getCurrencyList();

    interface AiOEconomy {

        Economy getEconomy();

        String getCurrencyKey();

        void disable();

        boolean isTransferable();
    }
}
