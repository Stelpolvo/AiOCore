package com.github.stelpolvo.aiocore.api.data;

import java.util.Map;

public interface EconomyData {
    Double get(String currency);

    Map<String, Double> get();

    void set(String currency, double value);

    boolean isCurrent();

    void setCurrent(boolean current);

    boolean isInit();

    void setInit(boolean init);
}
