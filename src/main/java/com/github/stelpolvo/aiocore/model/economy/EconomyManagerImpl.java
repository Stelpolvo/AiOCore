package com.github.stelpolvo.aiocore.model.economy;

import com.github.stelpolvo.aiocore.api.EconomyManager;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyManagerImpl implements EconomyManager {
    private final Map<String, AiOEconomy> economyMap = new HashMap<>();
    private String vault;

    public void load(ConfigurationSection section, PlayerDataManager dataManager, Logger logger) {
        if (section.getBoolean("enabled", false)){
            return;
        }
        this.vault = section.getString("vault");
        ConfigurationSection currencies = section.getConfigurationSection("currencies");
        if (currencies != null) {
            currencies.getKeys(false).forEach(key -> {
                try {
                    AiOEconomy economy = new AiOEconomyImpl(
                            dataManager, key,
                            currencies.getString(key+".name-singular"),
                            currencies.getString(key+".name-plural"),
                            currencies.getInt(key+".fractional-digits"),
                            currencies.getString(key+".format"),
                            currencies.getString(key+".symbol"),
                            currencies.getBoolean(key+".transferable")
                    );
                    economyMap.put(key, economy);
                }catch (Exception e){
                    logger.log(Level.SEVERE, "Failed to load economy for " + key, e);
                }
            });
        }
    }

    public void register(String currencyKey, AiOEconomy economy) {
        economyMap.put(currencyKey, economy);
    }

    public AiOEconomy getAiOEconomy(String currencyKey) {
        return economyMap.get(currencyKey);
    }

    public AiOEconomy getVaultEconomy() {
        return vault != null ? economyMap.get(vault) : null;
    }

    public List<String> getCurrencyList() {
        return economyMap.keySet().stream().toList();
    }

}
