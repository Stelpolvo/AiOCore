package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.ChatManager;

import java.util.List;

public class MessageStyleImpl extends TextStyleImpl implements ChatManager.MessageStyle {
    public MessageStyleImpl(String key, List<String> colors, String permission) {
        super(key, colors, permission);
    }
}
