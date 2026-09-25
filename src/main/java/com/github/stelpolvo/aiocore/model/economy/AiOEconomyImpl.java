package com.github.stelpolvo.aiocore.model.economy;

import com.github.stelpolvo.aiocore.api.EconomyManager;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.EconomyData;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

import java.util.Collections;
import java.util.List;

public class AiOEconomyImpl extends AbstractEconomy implements EconomyManager.AiOEconomy {
    private final String currencyKey;
    
    private final String currencyNameSingular;

    private final String currencyNamePlural;
    
    private final PlayerDataManager manager;

    private volatile boolean enabled = true;

    private final int fractionalDigits;

    private final String fractionalDigitsFormat;

    private final String format;

    private final String symbol;

    private final boolean transferable;

    public AiOEconomyImpl(PlayerDataManager manager, String currencyKey, String currencyNameSingular, String currencyNamePlural, int fractionalDigits, String format, String symbol, boolean transferable) {
        this.currencyKey = currencyKey;
        this.currencyNameSingular = currencyNameSingular;
        this.currencyNamePlural = currencyNamePlural;
        this.fractionalDigits = Math.max(0, fractionalDigits);
        this.manager = manager;
        this.format = format;
        this.symbol = symbol;
        this.transferable = transferable;
        this.fractionalDigitsFormat = "%." + fractionalDigits + "f";
    }

    @Override
    public Economy getEconomy() {
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
    }

    public String getCurrencyKey() {
        return currencyKey;
    }

    public boolean isTransferable() {
        return this.transferable;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "AiOEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return fractionalDigits;
    }

    @Override
    public String format(double amount) {
        return format.replaceAll("%amount%", String.format(this.fractionalDigitsFormat, amount));
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public String currencyNamePlural() {
        return currencyNamePlural;
    }

    @Override
    public String currencyNameSingular() {
        return currencyNameSingular;
    }

    @Override
    public boolean hasAccount(String playerName) {
        return playerName != null;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(playerName, currencyKey);
    }

    @Override
    public double getBalance(String playerName, String currency) {
        if (playerName == null) {
            return 0.0;
        }

        EconomyData data = manager.getByName(playerName).getEconomyData();
        if (data == null) {
            return 0.0;
        }

        synchronized (data) {
            return data.get(currencyKey);
        }
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(playerName, currencyKey, amount);
    }

    @Override
    public boolean has(String playerName, String currency, double amount) {
        if (amount < 0) {
            return false;
        }
        return getBalance(playerName, currency) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(playerName, currencyKey, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String currency, double amount) {
        if (playerName == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Player name cannot be null.");
        }
        if (amount < 0) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Cannot withdraw negative amount.");
        }

        EconomyData data = manager.getByName(playerName).getEconomyData();
        if (data == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Account not found.");
        }

        synchronized (data) {
            double balance = data.get(currencyKey);
            if (balance < amount) {
                return new EconomyResponse(0, balance, ResponseType.FAILURE, "Insufficient funds.");
            }

            double newBalance = balance - amount;
            data.set(currencyKey, newBalance);
            data.update();

            return new EconomyResponse(amount, newBalance, ResponseType.SUCCESS, null);
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(playerName, currencyKey, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String currency, double amount) {
        if (playerName == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Player name cannot be null.");
        }
        if (amount < 0) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Cannot deposit negative amount.");
        }

        EconomyData data = manager.getByName(playerName).getEconomyData();
        if (data == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Account not found.");
        }

        synchronized (data) {
            double balance = data.get(currencyKey);
            double newBalance = balance + amount;

            data.set(currencyKey, newBalance);
            data.update();

            return new EconomyResponse(amount, newBalance, ResponseType.SUCCESS, null);
        }
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String player) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }
        manager.getByName(playerName).getEconomyData().set(currencyKey, 0);
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }


    private EconomyResponse notImplemented() {
        return new EconomyResponse(
                0,
                0,
                ResponseType.NOT_IMPLEMENTED,
                "AiOEconomy does not support banks."
        );
    }
}