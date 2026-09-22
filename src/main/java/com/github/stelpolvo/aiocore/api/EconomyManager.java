package com.github.stelpolvo.aiocore.api;

import net.milkbowl.vault.economy.Economy;

public interface EconomyManager {

    AiOEconomy getAiOEconomy(String currencyKey);

    interface AiOEconomy {

        Economy getEconomy();

        void disable();
    }
}
