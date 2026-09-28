package com.github.stelpolvo.aiocore.api.data;

public interface AbstractData {
    boolean isCurrent();

    void setCurrent(boolean current);

    boolean isInit();

    void setInit(boolean init);
}
