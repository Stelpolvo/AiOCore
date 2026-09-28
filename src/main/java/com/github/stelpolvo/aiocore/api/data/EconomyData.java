package com.github.stelpolvo.aiocore.api.data;

import java.util.Map;

public interface EconomyData extends AbstractData {
    Double get(String currency);

    Map<String, Double> get();

    void set(String currency, double value);


}
