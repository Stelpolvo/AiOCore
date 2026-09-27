package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.ChatManager;

public class ChannelImpl implements ChatManager.Channel {
    private final String key;
    private final String name;
    private final String permission;
    public ChannelImpl(String key, String name, String permission) {
        this.key = key;
        this.name = name;
        this.permission = permission;
    }
    public String key() {
        return key;
    }

    public String permission() {
        return permission;
    }

    public String name() {
        return name;
    }
}
