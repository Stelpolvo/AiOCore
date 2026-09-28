package com.github.stelpolvo.aiocore.model.economy;

import com.github.stelpolvo.aiocore.api.data.EconomyData;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyDataImpl implements EconomyData {

    private final Map<String, Double> data;
    private volatile boolean isCurrent = true;
    private volatile boolean isInit = false;

    public EconomyDataImpl(Map<String, Double> data) {
        this.data = data == null
                ? new ConcurrentHashMap<>()
                : new ConcurrentHashMap<>(data);
    }

    @Override
    public Double get(String currency) {
        return data.getOrDefault(currency, 0.0);
    }

    @Override
    public Map<String, Double> get() {
        return Collections.unmodifiableMap(data);
    }

    @Override
    public void set(String currency, double value) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency key is null or blank");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Invalid economy value for '"
                    + currency + "': " + value);
        }
        data.put(currency, value);
        this.isCurrent = false;
    }

    @Override
    public boolean isCurrent() {
        return isCurrent;
    }

    @Override
    public void setCurrent(boolean current) {
        this.isCurrent = current;
    }

    @Override
    public boolean isInit() {
        return isInit;
    }

    @Override
    public void setInit(boolean init) {
        this.isInit = init;
    }

    @Override
    public String toString() {
        return "EconomyDataImpl{" +
                "data=" + data +
                ", isCurrent=" + isCurrent +
                ", isInit=" + isInit +
                '}';
    }
}