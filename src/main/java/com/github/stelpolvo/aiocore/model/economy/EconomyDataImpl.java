package com.github.stelpolvo.aiocore.model.economy;

import com.github.stelpolvo.aiocore.api.data.EconomyData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EconomyDataImpl implements EconomyData {
    private final Map<String, Double> data;
    private boolean isCurrent = true;

    public EconomyDataImpl(Map<String, Double> data){
        this.data = Objects.requireNonNullElse(data, new HashMap<>());
    }

    public Double get(String currency){
        return data.getOrDefault(currency, 0.0);
    }

    public Map<String, Double> get(){
        return new HashMap<>(data);
    }

    public void set(String currency, double value){
        data.put(currency, value);
        this.isCurrent = false;
    }

    public boolean isCurrent(){
        return isCurrent;
    }

    public void update(){
        isCurrent = true;
    }
}
