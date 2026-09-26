package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.ChatManager;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.ChatData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

public class ChatManagerImpl implements ChatManager {
    private final PlayerDataManager manager;
    private final Map<String, NameStyle> nameStylesMap = new HashMap<>();
    private final Map<String, MessageStyle> messageStylesMap = new HashMap<>();
    private final Map<String, ChatStyle> chatStylesMap = new HashMap<>();
    private final Map<String, SoundStyle> soundStylesMap = new HashMap<>();
    private boolean isEnabled = false;
    public ChatManagerImpl(PlayerDataManager manager, ConfigurationSection section) {
        this.manager = manager;
        if (section == null || !section.getBoolean("enabled")){
            return;
        }else {
            isEnabled = true;
        }
        ConfigurationSection nameStyle = section.getConfigurationSection("styles.name");
        if (nameStyle != null) {
            nameStyle.getKeys(false).forEach(k -> nameStylesMap.put(k, new NameStyleImpl(k, nameStyle.getStringList(k+".color"),nameStyle.getString(k+".permission"))));
        }
        ConfigurationSection messageStyle = section.getConfigurationSection("styles.message");
        if (messageStyle != null) {
            messageStyle.getKeys(false).forEach(k -> messageStylesMap.put(k, new MessageStyleImpl(k, messageStyle.getStringList(k+".color"),messageStyle.getString(k+".permission"))));
        }
        ConfigurationSection chatStyle = section.getConfigurationSection("styles.chat");
        if (chatStyle != null) {
            chatStyle.getKeys(false).forEach(k -> chatStylesMap.put(k, new ChatStyleImpl(k, chatStyle.getString(k+".format"),chatStyle.getString(k+".permission"))));
        }
        ConfigurationSection soundStyle = section.getConfigurationSection("styles.sound");
        if (soundStyle != null) {
            soundStyle.getKeys(false).forEach(k -> soundStylesMap.put(k, new SoundStyleImpl(k, soundStyle.getString(k+".sound"),soundStyle.getString(k+".permission"))));
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isEnabled) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        String coloredName = player.getDisplayName();
        String coloredMessage = event.getMessage();
        String bcMessage = "<%player%> %message%";
        ChatData data = manager.getPlayerData().get(player.getUniqueId()).getChatData();
        String nameColorKey = data.getNameStyle();
        if (nameColorKey != null){
            NameStyle style = nameStylesMap.get(nameColorKey);
            if (style != null){
                coloredName = style.parse(player, coloredName);
            }
        }
        String messageColorKey = data.getMessageStyle();
        if (messageColorKey != null){
            MessageStyle style = messageStylesMap.get(messageColorKey);
            if (style != null){
                coloredMessage = style.parse(player, coloredMessage);
            }
        }
        String chatKey = data.getChatStyle();
        boolean successParse = false;
        if (chatKey != null){
            ChatStyle style = chatStylesMap.get(chatKey);
            if (style != null){
                bcMessage = style.parse(player, bcMessage).replaceAll("%player%", coloredName).replaceAll("%message%", coloredMessage);
                successParse = true;
            }
        }
        if (!successParse){
            bcMessage = bcMessage.replaceAll("%player%", coloredName).replaceAll("%message%", coloredMessage);
        }
        String soundKey = data.getSoundStyle();
        SoundStyle soundStyle = soundStylesMap.get(soundKey);
        Bukkit.broadcastMessage(bcMessage);
        if (soundKey != null){
            event.getRecipients().forEach(r -> r.playSound(r.getLocation(), soundStyle.getSound(), soundStyle.getVolume(), soundStyle.getPitch()));
        }

    }


    public ChatStyle getChatStyle(String key) {
        return chatStylesMap.get(key);
    }

    public Map<String, ChatStyle> getChatStyles() {
        return chatStylesMap;
    }

    public NameStyle getNameStyle(String key) {
        return nameStylesMap.get(key);
    }

    public Map<String, NameStyle> getNameStyles() {
        return nameStylesMap;
    }

    public MessageStyle getMessageStyle(String key) {
        return messageStylesMap.get(key);
    }

    public Map<String, MessageStyle> getMessageStyles() {
        return messageStylesMap;
    }

    public SoundStyle getSoundStyle(String key) {
        return soundStylesMap.get(key);
    }

    public Map<String, SoundStyle> getSoundStyles() {
        return soundStylesMap;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
