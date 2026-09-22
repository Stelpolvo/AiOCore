package com.github.stelpolvo.aiocore.model.economy;

import com.github.stelpolvo.aiocore.api.EconomyManager;

import java.util.HashMap;
import java.util.Map;

public class EconomyManagerImpl implements EconomyManager {
    private final Map<String, AiOEconomy> economyMap = new HashMap<>();
    public AiOEconomy getAiOEconomy(String currencyKey) {
        return economyMap.get(currencyKey);
    }
}
