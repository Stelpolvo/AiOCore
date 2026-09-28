package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.data.ChatData;

public class ChatDataImpl implements ChatData {

    private volatile String nameKey;
    private volatile String messageKey;
    private volatile String chatKey;
    private volatile String soundKey;
    private volatile String channelKey;
    private volatile boolean isCurrent = true;
    private volatile boolean isInit = false;

    public ChatDataImpl(String nameKey, String messageKey, String chatKey,
                        String soundKey, String channelKey) {
        this.nameKey = normalize(nameKey);
        this.messageKey = normalize(messageKey);
        this.chatKey = normalize(chatKey);
        this.soundKey = normalize(soundKey);
        this.channelKey = normalize(channelKey);
    }

    public static ChatDataImpl defaultData() {
        return new ChatDataImpl(
                ChatData.DEFAULT_KEY, ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY, ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY);
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? ChatData.DEFAULT_KEY : value;
    }

    @Override
    public String getNameStyle() {
        return nameKey;
    }

    @Override
    public String getMessageStyle() {
        return messageKey;
    }

    @Override
    public String getChatStyle() {
        return chatKey;
    }

    @Override
    public String getSoundStyle() {
        return soundKey;
    }

    @Override
    public String getChannel() {
        return channelKey;
    }

    @Override
    public void setNameStyle(String nameStyle) {
        this.nameKey = normalize(nameStyle);
        this.isCurrent = false;
    }

    @Override
    public void setMessageStyle(String messageStyle) {
        this.messageKey = normalize(messageStyle);
        this.isCurrent = false;
    }

    @Override
    public void setChatStyle(String chatStyle) {
        this.chatKey = normalize(chatStyle);
        this.isCurrent = false;
    }

    @Override
    public void setSoundStyle(String soundStyle) {
        this.soundKey = normalize(soundStyle);
        this.isCurrent = false;
    }

    @Override
    public void setChannel(String channel) {
        this.channelKey = normalize(channel);
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
        return "ChatDataImpl{" +
                "name='" + nameKey + '\'' +
                ", message='" + messageKey + '\'' +
                ", chat='" + chatKey + '\'' +
                ", sound='" + soundKey + '\'' +
                ", channel='" + channelKey + '\'' +
                ", isCurrent=" + isCurrent +
                ", isInit=" + isInit +
                '}';
    }
}