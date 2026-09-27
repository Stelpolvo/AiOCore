package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.AiO;
import com.github.stelpolvo.aiocore.api.ChatManager;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatManagerImpl implements ChatManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private static final String CLS_ASYNC_CHAT_EVENT =
            "io.papermc.paper.event.player.AsyncChatEvent";
    private static final String CLS_CHAT_RENDERER =
            "io.papermc.paper.chat.ChatRenderer";

    public static final boolean HAS_PAPER_CHAT;

    static {
        boolean present;
        try {
            Class.forName(CLS_ASYNC_CHAT_EVENT);
            present = true;
        } catch (Throwable t) {
            present = false;
        }
        HAS_PAPER_CHAT = present;
    }

    private final PlayerDataManager manager;
    private final Map<String, NameStyle> nameStylesMap = new HashMap<>();
    private final Map<String, MessageStyle> messageStylesMap = new HashMap<>();
    private final Map<String, ChatStyle> chatStylesMap = new HashMap<>();
    private final Map<String, SoundStyle> soundStylesMap = new HashMap<>();
    private final Map<String, Channel> channelsMap = new HashMap<>();
    private final Map<UUID, Channel> channelPlayerMap = new HashMap<>();
    private final Logger logger;
    private final AiO aio;
    private final BukkitAudiences audiences;

    private boolean isEnabled = false;
    private boolean crossServer = false;
    private String serverId;

    public ChatManagerImpl(AiO aio, PlayerDataManager manager, ConfigurationSection section, Logger logger) {
        this.aio = aio;
        this.manager = manager;
        this.logger = logger;
        this.audiences = BukkitAudiences.create(aio.getJavaPlugin());

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
        registerPaperChatEvent(aio);
    }

    @SuppressWarnings("unchecked")
    private void registerPaperChatEvent(AiO plugin) {
        if (!HAS_PAPER_CHAT) return;
        try {
            Class<? extends Event> cls =
                    (Class<? extends Event>) Class.forName(CLS_ASYNC_CHAT_EVENT);
            Bukkit.getPluginManager().registerEvent(
                    cls,
                    this,
                    EventPriority.HIGHEST,
                    this,
                    plugin.getJavaPlugin()
            );
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to register Paper AsyncChatEvent", t);
        }
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        if (!isEnabled) return;
        if (HAS_PAPER_CHAT) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Channel channel = channelPlayerMap.get(player.getUniqueId());
        broadcastBukkit(player.getUniqueId(), player.getName(),
                event.getMessage(), event.getRecipients(), serverId, channel.key());

        if (crossServer) {
            sendCrossServerMessage(player, event.getMessage(), channel.key());
        }
    }

    @Override
    public void execute(@NotNull Listener listener, @NotNull Event event) throws EventException {
        if (!isEnabled) return;

        Player player = readPlayer(event);
        if (player == null) return;

        Component message = readMessage(event);
        if (message == null) return;

        String plain = PlainTextComponentSerializer.plainText().serialize(message);
        if (plain.isEmpty()) return;

        ChatData data = resolveChatData(player.getUniqueId());
        if (data == null) return;

        Component rendered = buildComponent(player, data, plain, serverId);
        Channel senderChannel = channelPlayerMap.get(player.getUniqueId());

        applyRenderer(event, rendered, senderChannel);
        playSoundForViewers(event, data, senderChannel);

        if (crossServer) {
            sendCrossServerMessage(player, plain, senderChannel.key());
        }
    }

    private Player readPlayer(Event event) {
        try {
            Object result = event.getClass().getMethod("getPlayer").invoke(event);
            return result instanceof Player p ? p : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private Component readMessage(Event event) {
        try {
            Object result = event.getClass().getMethod("message").invoke(event);
            return result instanceof Component c ? c : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private void applyRenderer(Event event, Component component, Channel senderChannel) {
        try {
            Class<?> rendererCls = Class.forName(CLS_CHAT_RENDERER);
            Object proxy = Proxy.newProxyInstance(
                    rendererCls.getClassLoader(),
                    new Class<?>[]{ rendererCls },
                    (p, method, args) -> {
                        String name = method.getName();
                        return switch (name) {
                            case "render" -> renderForViewer(args, component, senderChannel);
                            case "equals" -> p == args[0];
                            case "hashCode" -> System.identityHashCode(p);
                            case "toString" -> "AiOCoreChatRenderer";
                            default -> null;
                        };
                    });
            event.getClass().getMethod("renderer", rendererCls).invoke(event, proxy);
        } catch (Throwable ignored) {
        }
    }

    private Component renderForViewer(Object[] args, Component defaultComponent, Channel senderChannel) {
        if (args == null || args.length < 4) return defaultComponent;
        if (!(args[3] instanceof Player viewer)) return defaultComponent;

        Channel viewerChannel = channelPlayerMap.get(viewer.getUniqueId());
        if (!Objects.equals(viewerChannel, senderChannel)) {
            return Component.empty();
        }
        return defaultComponent;
    }

    private void playSoundForViewers(Event event, ChatData data, Channel senderChannel) {
        SoundStyle sound = resolveSound(data);
        if (sound == null) return;

        try {
            Object viewers = event.getClass().getMethod("viewers").invoke(event);
            if (!(viewers instanceof Set<?> set)) return;

            for (Object v : set) {
                if (!(v instanceof Player p)) continue;
                Channel viewerChannel = channelPlayerMap.get(p.getUniqueId());
                if (!Objects.equals(viewerChannel, senderChannel)) continue;

                Bukkit.getScheduler().runTask(aio.getJavaPlugin(), () ->
                        p.playSound(p.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch()));
            }
        } catch (Throwable ignored) {
        }
    }

    public ChatData resolveChatData(UUID uuid) {
        PlayerData pd = manager.getPlayerData().get(uuid);
        if (pd == null) pd = manager.getByUUID(uuid);
        return pd != null ? pd.getChatData() : null;
    }

    public Component buildComponent(OfflinePlayer player, ChatData data,
                                    String message, String sourceServerId) {
        return LEGACY.deserialize(formatMessage(player, data, message, sourceServerId));
    }

    public void broadcastBukkit(UUID uuid, String name, String message,
                                Set<Player> recipients, String sourceServerId, String channel) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        ChatData data = resolveChatData(uuid);
        if (data == null) data = defaultChatData();

        Component component = LEGACY.deserialize(
                formatMessage(offline, name, data, message, sourceServerId));
        SoundStyle sound = resolveSound(data);

        Collection<? extends Player> targets = recipients != null ? recipients : Bukkit.getOnlinePlayers();
        Channel channelObj = channelsMap.get(channel);
        for (Player r : targets) {
            if (channelPlayerMap.get(r.getUniqueId()).equals(channelObj)) {
                audiences.player(r).sendMessage(component);
                if (sound != null) {
                    r.playSound(r.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch());
                }
            }
        }
    }

    private String formatMessage(OfflinePlayer player, ChatData data, String message, String sourceServerId) {
        String rawName = player.getName() != null ? player.getName() : "unknown";
        return formatMessage(player, rawName, data, message, sourceServerId);
    }

    private String formatMessage(OfflinePlayer player, String rawName, ChatData data,
                                 String message, String sourceServerId) {
        String coloredName = rawName;
        String coloredMessage = message;

        if (data.getNameStyle() != null) {
            NameStyle style = nameStylesMap.get(data.getNameStyle());
            if (style != null) coloredName = style.parse(player, coloredName);
        }
        if (data.getMessageStyle() != null) {
            MessageStyle style = messageStylesMap.get(data.getMessageStyle());
            if (style != null) coloredMessage = style.parse(player, coloredMessage);
        }

        String template = null;
        if (data.getChatStyle() != null) {
            ChatStyle style = chatStylesMap.get(data.getChatStyle());
            if (style != null) template = style.parse(player, style.format());
        }
        if (template == null) template = "<%player%> %message%";

        return template
                .replace("&", "§")
                .replace("%player%", coloredName)
                .replace("%message%", coloredMessage)
                .replace("%server%", sourceServerId != null ? sourceServerId : "");
    }

    private SoundStyle resolveSound(ChatData data) {
        if (data.getSoundStyle() == null) return null;
        return soundStylesMap.get(data.getSoundStyle());
    }

    private ChatData defaultChatData() {
        return new ChatDataImpl(
                ChatData.DEFAULT_KEY, ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY, ChatData.DEFAULT_KEY,
                ChatData.DEFAULT_KEY);
    }

    public void sendCrossServerMessage(Player player, String message, String channel) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AiOCoreChat");

        ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
        DataOutputStream msgOut = new DataOutputStream(msgBytes);
        try {
            msgOut.writeUTF(player.getUniqueId().toString());
            msgOut.writeUTF(serverId != null ? serverId : "unknown");
            msgOut.writeUTF(channel);
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
            String chatChannel = msgIn.readUTF();
            String senderName = msgIn.readUTF();
            String chatMessage = msgIn.readUTF();

            if (serverId != null && serverId.equals(remoteServer)) return;

            broadcastBukkit(UUID.fromString(senderUuid), senderName, chatMessage, null, remoteServer, chatChannel);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read cross-server chat message", e);
        }
    }


    public void close() {
        if (audiences != null) {
            audiences.close();
        }
    }

    @Override public ChatStyle getChatStyle(String key) { return chatStylesMap.get(key); }
    @Override public Map<String, ChatStyle> getChatStyles() { return chatStylesMap; }
    @Override public NameStyle getNameStyle(String key) { return nameStylesMap.get(key); }
    @Override public Map<String, NameStyle> getNameStyles() { return nameStylesMap; }
    @Override public MessageStyle getMessageStyle(String key) { return messageStylesMap.get(key); }
    @Override public Map<String, MessageStyle> getMessageStyles() { return messageStylesMap; }
    @Override public SoundStyle getSoundStyle(String key) { return soundStylesMap.get(key); }
    @Override public Map<String, SoundStyle> getSoundStyles() { return soundStylesMap; }
    @Override public Channel getChannel(String key) { return channelsMap.get(key); }
    @Override public Map<String, Channel> getChannels() { return channelsMap; }
    @Override public boolean isEnabled() { return isEnabled; }
}