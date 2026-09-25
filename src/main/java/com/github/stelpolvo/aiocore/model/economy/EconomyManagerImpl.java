package com.github.stelpolvo.aiocore.model.economy;

import com.github.stelpolvo.aiocore.api.EconomyManager;
import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyManagerImpl implements EconomyManager {
    private final Map<String, AiOEconomy> economyMap = new HashMap<>();
    private String vault;
    private boolean isEnabled = false;
    private PlayerDataManager manager;

    public void load(ConfigurationSection section, PlayerDataManager dataManager, Logger logger, Messenger messenger) {
        this.manager = dataManager;
        this.isEnabled = section.getBoolean("enabled", false);
        if (!this.isEnabled){
            return;
        }
        this.vault = section.getString("vault");
        messenger.send(Bukkit.getConsoleSender(), Messenger.ECONOMY_ENABLE_VAULT, "currency", this.vault);
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
                    messenger.send(Bukkit.getConsoleSender(), Messenger.SUCCESS_LOAD_ECONOMY, "currency", economy.getCurrencyKey());
                }catch (Exception e){
                    logger.log(Level.SEVERE, "Failed to load economy for " + key, e);
                }
            });
        }
    }

    public void register(String currencyKey, AiOEconomy economy) {
        economyMap.put(currencyKey, economy);
    }

    public boolean isEnabled() {
        return this.isEnabled;
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

    @Override
    public String getPlaceholderPrefix(){
        return "economy";
    }

    private final Map<String, OfflinePlayer> nameCache = new ConcurrentHashMap<>();

    private OfflinePlayer resolveOfflinePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        String key = name.toLowerCase(Locale.ROOT);
        OfflinePlayer cached = nameCache.get(key);
        if (cached != null) return cached;

        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            String opName = op.getName();
            if (opName != null && opName.equalsIgnoreCase(name)) {
                nameCache.put(key, op);
                return op;
            }
        }
        return null;
    }

    @Override
    public String onPlaceholderRequest(OfflinePlayer player, @NotNull String[] params) {
        String currency = params[1];
        EconomyManager.AiOEconomy eco = getAiOEconomy(currency);
        if (eco == null) {
            return null;
        }
        OfflinePlayer target = player;
        String modifier;

        if (params.length >= 4 && "player".equalsIgnoreCase(params[2])) {
            String name = params[3];
            target = resolveOfflinePlayer(name);
            modifier = params.length >= 5 ? params[4] : "";
        } else {
            modifier = params.length >= 3 ? params[2] : "";
        }

        if (target == null || !target.hasPlayedBefore()) {
            return "";
        }

        PlayerData pd = manager.getByUUID(target.getUniqueId());
        if (pd == null) {
            return "";
        }

        double amount = pd.getEconomyData().get(currency);

        String sym = eco.getSymbol();

        return switch (modifier.toLowerCase(Locale.ROOT)) {
            case ""          -> String.valueOf(amount);
            case "formatted" -> eco.getEconomy().format(amount);
            case "display"   -> eco.getFormat()
                    .replace("%amount%", eco.getEconomy().format(amount))
                    .replace("%symbol%", sym);
            case "symbol"    -> sym;
            case "name"      -> eco.getEconomy().currencyNamePlural();
            default          -> null;
        };
    }
}
