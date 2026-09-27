package com.github.stelpolvo.aiocore.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Map;

public interface ChatManager extends Listener, PluginMessageListener {
    ChatStyle getChatStyle(String key);
    Map<String, ChatStyle> getChatStyles();
    NameStyle getNameStyle(String key);
    Map<String, NameStyle> getNameStyles();
    MessageStyle getMessageStyle(String key);
    Map<String, MessageStyle> getMessageStyles();
    SoundStyle getSoundStyle(String key);
    Map<String, SoundStyle> getSoundStyles();
    Channel getChannel(String key);
    Map<String, Channel> getChannels();
    boolean isEnabled();
    interface Style {
        String key();
        String parse(OfflinePlayer player, String raw);
        String permission();
    }
    interface NameStyle extends Style {
    }
    interface MessageStyle extends Style {
    }
    interface ChatStyle extends Style {
        String format();
    }
    interface SoundStyle {
        String key();
        Sound getSound();
        float getPitch();
        float getVolume();
        String permission();
    }
    interface Channel {
        String key();
        String name();
        String permission();
    }
    enum CrossServerType {
        PLUGIN_MESSAGE,
        REDIS
    }
}
