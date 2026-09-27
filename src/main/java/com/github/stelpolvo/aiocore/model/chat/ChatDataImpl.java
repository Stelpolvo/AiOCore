package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.data.ChatData;

public class ChatDataImpl implements ChatData {
    private String nameKey;
    private String messageKey;
    private String chatKey;
    private String soundKey;
    private String channelKey;
    private boolean isCurrent = true;
    private boolean isInit = false;
    public ChatDataImpl(String nameKey, String messageKey, String chatKey, String soundKey, String channelKey) {
        this.nameKey = nameKey;
        this.messageKey = messageKey;
        this.chatKey = chatKey;
        this.soundKey = soundKey;
        this.channelKey = channelKey;
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

    public String getChannel() {
        return channelKey;
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

    public void setChannel(String channel) {
        this.channelKey = channel;
        this.isCurrent = false;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        this.isCurrent = current;
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        this.isInit = init;
    }

    @Override
    public String toString() {
        return "ChatDataImpl{" +
                "nameKey='" + nameKey + '\'' +
                ", messageKey='" + messageKey + '\'' +
                ", chatKey='" + chatKey + '\'' +
                ", soundKey='" + soundKey + '\'' +
                ", channelKey='" + channelKey + '\'' +
                ", isCurrent=" + isCurrent +
                ", isInit=" + isInit +
                '}';
    }
}
