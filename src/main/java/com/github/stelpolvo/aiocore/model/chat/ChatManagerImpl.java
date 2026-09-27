package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.AiO;
import com.github.stelpolvo.aiocore.api.ChatManager;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatManagerImpl implements ChatManager {
    private final PlayerDataManager manager;
    private final Map<String, NameStyle> nameStylesMap = new HashMap<>();
    private final Map<String, MessageStyle> messageStylesMap = new HashMap<>();
    private final Map<String, ChatStyle> chatStylesMap = new HashMap<>();
    private final Map<String, SoundStyle> soundStylesMap = new HashMap<>();
    private final Map<String, Channel> channelsMap = new HashMap<>();
    private final Map<OfflinePlayer, Channel> channelPlayerMap = new HashMap<>();
    private final Logger logger;
    private final AiO aio;
    private boolean isEnabled = false;
    private boolean crossServer = false;
    private CrossServerType type;
    public ChatManagerImpl(AiO aio, PlayerDataManager manager, ConfigurationSection section, Logger logger) {
        this.aio = aio;
        this.manager = manager;
        this.logger = logger;
        if (section == null || !section.getBoolean("enabled")){
            return;
        }else {
            isEnabled = true;
        }
        this.crossServer = section.getBoolean("cross-server");
        if (crossServer) {
            this.type = CrossServerType.valueOf(section.getString("cross-server.type", "PLUGIN_MESSAGE").toUpperCase(Locale.ROOT));
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
        ConfigurationSection channelSec = section.getConfigurationSection("channels");
        if (channelSec != null) {
            channelSec.getKeys(false).forEach(k -> channelsMap.put(k, new ChannelImpl(k, channelSec.getString(k+".name", k) ,channelSec.getString(k+".permission"))));
        }

        Bukkit.getOnlinePlayers().forEach(this::onJoin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        onJoin(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        channelPlayerMap.remove(event.getPlayer());
    }

    public void onJoin(Player player){
        String channel = manager.getByUUID(player.getUniqueId()).getChatData().getChannel();
        if (channelsMap.containsKey(channel)) {
            channelPlayerMap.put(player, channelsMap.get(channel));
        }else {
            channelPlayerMap.put(player, channelsMap.get(ChatData.DEFAULT_KEY));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isEnabled) return;
        event.setCancelled(true);
        broadcast(event.getPlayer().getUniqueId(), event.getPlayer().getName(), event.getMessage(), event.getRecipients());
        if (crossServer){
            sendCrossServerMessage(event.getPlayer(), event.getMessage());
        }
    }

    private void broadcast(UUID uuid, String name, String message, Set<Player> recipients){
        String coloredName = name;
        String coloredMessage = message;
        String bcMessage = "<%player%> %message%";
        ChatData data = manager.getPlayerData().get(uuid).getChatData();
        String nameColorKey = data.getNameStyle();
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
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
                bcMessage = style.format();
                bcMessage = style.parse(player, bcMessage).replaceAll("%player%", coloredName).replaceAll("%message%", coloredMessage).replaceAll("&", "§");
                successParse = true;
            }
        }
        if (!successParse){
            bcMessage = bcMessage.replaceAll("%player%", coloredName).replaceAll("%message%", coloredMessage);
        }
        String soundKey = data.getSoundStyle();
        SoundStyle soundStyle = soundStylesMap.get(soundKey);
        Channel channel = channelPlayerMap.get(player);
        if (soundKey != null){
            String finalBcMessage = bcMessage;
            if (recipients != null){
                recipients.stream().filter(p -> channelPlayerMap.get(p).equals(channel)).forEach(r -> {
                    r.sendMessage(finalBcMessage);
                    r.playSound(r.getLocation(), soundStyle.getSound(), soundStyle.getVolume(), soundStyle.getPitch());
                });
            }else {
                Bukkit.getOnlinePlayers().stream().filter(p -> channelPlayerMap.get(p).equals(channel)).forEach(r -> {
                    r.sendMessage(finalBcMessage);
                    r.playSound(r.getLocation(), soundStyle.getSound(), soundStyle.getVolume(), soundStyle.getPitch());
                });
            }
        }
    }

    public void sendCrossServerMessage(Player player, String message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");

        out.writeUTF("AiOCoreChat");

        ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
        DataOutputStream msgOut = new DataOutputStream(msgBytes);
        try {
            msgOut.writeUTF(player.getUniqueId().toString());
            msgOut.writeUTF(player.getName());
            msgOut.writeUTF(message);
        }catch (Exception e){
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        byte[] msgData = msgBytes.toByteArray();
        out.writeShort(msgData.length);
        out.write(msgData);

        player.sendPluginMessage(aio.getJavaPlugin(), "BungeeCord", out.toByteArray());
    }

    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals("BungeeCord") && crossServer) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("AiOCoreChat")) {
            short len = in.readShort();
            byte[] msgBytes = new byte[len];
            in.readFully(msgBytes);

            DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgBytes));
            try {
                String senderUuid = msgIn.readUTF();
                String senderName = msgIn.readUTF();
                String chatMessage = msgIn.readUTF();
                broadcast(UUID.fromString(senderUuid), senderName, chatMessage, null);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
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

    public Channel getChannel(String key) {
        return channelsMap.get(key);
    }

    public Map<String, Channel> getChannels() {
        return channelsMap;
    }

    public boolean isEnabled() {
        return isEnabled;
    }


}
