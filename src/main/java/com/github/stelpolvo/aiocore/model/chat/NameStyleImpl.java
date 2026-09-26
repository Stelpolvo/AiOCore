package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.ChatManager;

import java.util.List;

public class NameStyleImpl extends TextStyleImpl implements ChatManager.NameStyle {

    public NameStyleImpl(String key, List<String> colors, String permission) {
        super(key, colors, permission);
    }
}
