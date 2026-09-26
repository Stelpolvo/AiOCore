package com.github.stelpolvo.aiocore.api.data;

public interface ChatData {
    String DEFAULT_KEY = "default";
    String getNameStyle();
    String getMessageStyle();
    String getChatStyle();
    String getSoundStyle();
    void setNameStyle(String nameStyle);
    void setMessageStyle(String messageStyle);
    void setChatStyle(String chatStyle);
    void setSoundStyle(String soundStyle);
    boolean isCurrent();
}
