package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.AiO;
import com.github.stelpolvo.aiocore.api.ChatManager;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
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
    private final Map<UUID, Channel> channelPlayerMap = new HashMap<>();
    private final Logger logger;
    private final AiO aio;

    private boolean isEnabled = false;
    private boolean crossServer = false;
    private String serverId;

    public ChatManagerImpl(AiO aio, PlayerDataManager manager, ConfigurationSection section, Logger logger) {
        this.aio = aio;
        this.manager = manager;
        this.logger = logger;

        if (section == null || !section.getBoolean("enabled")) {
            return;
        }
        isEnabled = true;

        ConfigurationSection crossSec = section.getConfigurationSection("cross-server");
        if (crossSec != null && crossSec.getBoolean("enabled")) {
            crossServer = true;
            this.serverId = crossSec.getString("server-id", "unknown");
        }

        ConfigurationSection nameStyle = section.getConfigurationSection("styles.name");
        if (nameStyle != null) {
            nameStyle.getKeys(false).forEach(k -> nameStylesMap.put(k,
                    new NameStyleImpl(k, nameStyle.getStringList(k + ".color"), nameStyle.getString(k + ".permission"))));
        }

        ConfigurationSection messageStyle = section.getConfigurationSection("styles.message");
        if (messageStyle != null) {
            messageStyle.getKeys(false).forEach(k -> messageStylesMap.put(k,
                    new MessageStyleImpl(k, messageStyle.getStringList(k + ".color"), messageStyle.getString(k + ".permission"))));
        }

        ConfigurationSection chatStyle = section.getConfigurationSection("styles.chat");
        if (chatStyle != null) {
            chatStyle.getKeys(false).forEach(k -> chatStylesMap.put(k,
                    new ChatStyleImpl(k, chatStyle.getString(k + ".format"), chatStyle.getString(k + ".permission"))));
        }

        ConfigurationSection soundStyle = section.getConfigurationSection("styles.sound");
        if (soundStyle != null) {
            soundStyle.getKeys(false).forEach(k -> soundStylesMap.put(k,
                    new SoundStyleImpl(k, soundStyle.getString(k + ".sound"), soundStyle.getString(k + ".permission"))));
        }

        ConfigurationSection channelSec = section.getConfigurationSection("channels");
        if (channelSec != null) {
            channelSec.getKeys(false).forEach(k -> channelsMap.put(k,
                    new ChannelImpl(k, channelSec.getString(k + ".name", k), channelSec.getString(k + ".permission"))));
        }

        Bukkit.getOnlinePlayers().forEach(this::onJoin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        onJoin(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        channelPlayerMap.remove(event.getPlayer().getUniqueId());
    }

    public void onJoin(Player player) {
        Channel fallback = channelsMap.get(ChatData.DEFAULT_KEY);

        PlayerData data = manager.getByUUID(player.getUniqueId());
        if (data == null || data.getChatData() == null) {
            channelPlayerMap.put(player.getUniqueId(), fallback);
            return;
        }

        String channelKey = data.getChatData().getChannel();
        Channel channel = channelKey != null ? channelsMap.get(channelKey) : null;
        channelPlayerMap.put(player.getUniqueId(), channel != null ? channel : fallback);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isEnabled) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        broadcast(player.getUniqueId(), player.getName(), event.getMessage(), event.getRecipients(), serverId);

        if (crossServer) {
            sendCrossServerMessage(player, event.getMessage());
        }
    }

    private void broadcast(UUID uuid, String name, String message, Set<Player> recipients, String sourceServerId) {
        PlayerData pd = manager.getPlayerData().get(uuid);
        if (pd == null) {
            pd = manager.getByUUID(uuid);
        }
        ChatData data = pd != null ? pd.getChatData() : null;

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

        String coloredName = name;
        String coloredMessage = message;

        if (data != null) {
            if (data.getNameStyle() != null) {
                NameStyle style = nameStylesMap.get(data.getNameStyle());
                if (style != null) {
                    coloredName = style.parse(offline, coloredName);
                }
            }
            if (data.getMessageStyle() != null) {
                MessageStyle style = messageStylesMap.get(data.getMessageStyle());
                if (style != null) {
                    coloredMessage = style.parse(offline, coloredMessage);
                }
            }
        }

        String bcMessage = null;
        if (data != null && data.getChatStyle() != null) {
            ChatStyle style = chatStylesMap.get(data.getChatStyle());
            if (style != null) {
                bcMessage = style.parse(offline, style.format());
            }
        }
        if (bcMessage == null) {
            bcMessage = "<%player%> %message%";
        }

        bcMessage = bcMessage
                .replace("&", "§")
                .replace("%player%", coloredName)
                .replace("%message%", coloredMessage)
                .replace("%server%", sourceServerId != null ? sourceServerId : "");

        SoundStyle soundStyle = null;
        if (data != null && data.getSoundStyle() != null) {
            soundStyle = soundStylesMap.get(data.getSoundStyle());
        }

        Channel channel = channelPlayerMap.get(uuid);

        Collection<? extends Player> targets = recipients != null ? recipients : Bukkit.getOnlinePlayers();
        for (Player r : targets) {
            Channel rChannel = channelPlayerMap.get(r.getUniqueId());
            if (!Objects.equals(rChannel, channel)) continue;

            r.sendMessage(bcMessage);
            if (soundStyle != null) {
                r.playSound(r.getLocation(), soundStyle.getSound(), soundStyle.getVolume(), soundStyle.getPitch());
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
            msgOut.writeUTF(serverId != null ? serverId : "unknown");
            msgOut.writeUTF(player.getName());
            msgOut.writeUTF(message);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to serialize cross-server message", e);
            return;
        }

        byte[] msgData = msgBytes.toByteArray();
        out.writeShort(msgData.length);
        out.write(msgData);

        player.sendPluginMessage(aio.getJavaPlugin(), "BungeeCord", out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!"BungeeCord".equals(channel)) return;
        if (!isEnabled || !crossServer) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (!"AiOCoreChat".equals(subChannel)) return;

        short len = in.readShort();
        byte[] msgBytes = new byte[len];
        in.readFully(msgBytes);

        DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgBytes));
        try {
            String senderUuid = msgIn.readUTF();
            String remoteServer = msgIn.readUTF();
            String senderName = msgIn.readUTF();
            String chatMessage = msgIn.readUTF();

            if (serverId != null && serverId.equals(remoteServer)) return;

            broadcast(UUID.fromString(senderUuid), senderName, chatMessage, null, remoteServer);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read cross-server chat message", e);
        }
    }

    @Override
    public ChatStyle getChatStyle(String key) { return chatStylesMap.get(key); }

    @Override
    public Map<String, ChatStyle> getChatStyles() { return chatStylesMap; }

    @Override
    public NameStyle getNameStyle(String key) { return nameStylesMap.get(key); }

    @Override
    public Map<String, NameStyle> getNameStyles() { return nameStylesMap; }

    @Override
    public MessageStyle getMessageStyle(String key) { return messageStylesMap.get(key); }

    @Override
    public Map<String, MessageStyle> getMessageStyles() { return messageStylesMap; }

    @Override
    public SoundStyle getSoundStyle(String key) { return soundStylesMap.get(key); }

    @Override
    public Map<String, SoundStyle> getSoundStyles() { return soundStylesMap; }

    @Override
    public Channel getChannel(String key) { return channelsMap.get(key); }

    @Override
    public Map<String, Channel> getChannels() { return channelsMap; }

    @Override
    public boolean isEnabled() { return isEnabled; }
}