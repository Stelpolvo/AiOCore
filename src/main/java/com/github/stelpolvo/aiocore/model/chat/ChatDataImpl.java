package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.data.ChatData;

public class ChatDataImpl implements ChatData {
    private String nameKey;
    private String messageKey;
    private String chatKey;
    private String soundKey;
    private boolean isCurrent = true;
    public ChatDataImpl(String nameKey, String messageKey, String chatKey, String soundKey) {
        this.nameKey = nameKey;
        this.messageKey = messageKey;
        this.chatKey = chatKey;
        this.soundKey = soundKey;
    }

    public String getNameStyle() {
        return nameKey;
    }

    public String getMessageStyle() {
        return messageKey;
    }

    public String getChatStyle() {
        return chatKey;
    }

    public String getSoundStyle() {
        return soundKey;
    }

    public void setNameStyle(String nameStyle) {
        this.nameKey = nameStyle;
        this.isCurrent = false;
    }

    public void setMessageStyle(String messageStyle) {
        this.messageKey = messageStyle;
        this.isCurrent = false;
    }

    public void setChatStyle(String chatStyle) {
        this.chatKey = chatStyle;
        this.isCurrent = false;
    }

    public void setSoundStyle(String soundStyle) {
        this.soundKey = soundStyle;
        this.isCurrent = false;
    }

    public boolean isCurrent() {
        return isCurrent;
    }
}
