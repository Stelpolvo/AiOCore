package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.ChatManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

public record ChatStyleImpl(String key, String format, String permission) implements ChatManager.ChatStyle {

    @Override
    public String parse(OfflinePlayer player, String raw) {
        return player != null ? PlaceholderAPI.setPlaceholders(player, raw) : raw;
    }

}
