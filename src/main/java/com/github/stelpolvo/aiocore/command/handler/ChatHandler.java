package com.github.stelpolvo.aiocore.command.handler;

import com.github.stelpolvo.aiocore.api.ChatManager;
import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.ChatData;
import com.github.stelpolvo.aiocore.command.CommandAPI;
import com.github.stelpolvo.aiocore.utils.PlayerUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * /aio chat &lt;name|message|chat|sound&gt;
 * /aio chat &lt;name|message|chat|sound&gt; set &lt;key&gt; [player]
 */
public class ChatHandler implements CommandAPI {
    public static final Map<String, String> TAB = ImmutableMap.of(
            "name", "aio.command.def.chat.name",
            "message", "aio.command.def.chat.message",
            "chat", "aio.command.def.chat.chat",
            "sound", "aio.command.def.chat.sound"
    );
    public static final List<String> TAB2 = ImmutableList.of("set");
    private final Messenger messenger;
    private final ChatManager chatManager;
    private final PlayerDataManager manager;
    public ChatHandler(Messenger messenger, ChatManager chatManager, PlayerDataManager manager) {
        this.messenger = messenger;
        this.chatManager = chatManager;
        this.manager = manager;
    }
    public boolean onPlayer(Player player, String[] args) {
        onCommand(player, args);
        return true;
    }

    public boolean onConsole(CommandSender sender, String[] args) {
        onCommand(sender, args);
        return true;
    }

    public void sendUsage(CommandSender sender){
        messenger.send(sender, "/aio chat <name|message|chat|sound>");
        messenger.send(sender, "/aio chat <name|message|chat|sound> set <key> [player]");
    }

    public void sendDisplay(Player sender, String type){
        switch (type) {
            case "name":
                messenger.send(sender, Messenger.CHAT_STYLE_DISPLAY);
                chatManager.getNameStyles().forEach((key, value) -> {
                    messenger.send(sender, Messenger.CHAT_STYLE_DISPLAY_LINE, "effect", value.parse(sender, "player_name"), "permission", value.permission());
                });
                break;
            case "message":
                messenger.send(sender, Messenger.CHAT_STYLE_DISPLAY);
                chatManager.getMessageStyles().forEach((key, value) -> {
                    messenger.send(sender, Messenger.CHAT_STYLE_DISPLAY_LINE, "effect", value.parse(sender, "player_name"), "permission", value.permission());
                });
                break;
            case "chat":
                messenger.send(sender, Messenger.CHAT_STYLE_DISPLAY);
                chatManager.getChatStyles().forEach((key, value) -> {
                    messenger.send(sender, Messenger.CHAT_STYLE_DISPLAY_LINE, "effect", value.parse(sender, "player_name"), "permission", value.permission());
                });
                break;
            default:
                break;
        }
    }

    private void onCommand(CommandSender sender, String[] args) {
        if (args.length == 2){
            if (sender instanceof Player player){
                sendDisplay(player, args[1].toLowerCase());
            }else {
                messenger.send(sender, Messenger.IS_PLAYER_COMMAND);
            }
            return;
        }
        if (args.length < 4){
            sendUsage(sender);
            return;
        }
        OfflinePlayer target;
        if (args.length >= 5){
            target = PlayerUtil.getIfPlayBefore(args[4]);
            if (target == null){
                messenger.send(sender, Messenger.PLAYER_NOT_EXIST);
                return;
            }
        }else if (sender instanceof Player){
            target = (OfflinePlayer) sender;
        }else {
           messenger.send(sender, Messenger.IS_PLAYER_COMMAND);
           return;
        }
        ChatData chatData = manager.getPlayerData().get(target.getUniqueId()).getChatData();
        String key = args[3];
        switch (args[1].toLowerCase()){
            case "name":
                ChatManager.NameStyle nameStyle = chatManager.getNameStyle(key);
                if (nameStyle == null){
                    messenger.send(sender, Messenger.CHAT_STYLE_NOT_EXIST);
                }else if (!target.isOnline() || target.getPlayer() == null){
                    messenger.send(sender, Messenger.PLAYER_NOT_ONLINE);
                }else if (!target.getPlayer().hasPermission(nameStyle.permission())){
                    messenger.send(sender, Messenger.CHAT_STYLE_NOT_PERMISSION, "permission", nameStyle.permission());
                }else {
                    chatData.setNameStyle(nameStyle.key());
                    messenger.send(sender, Messenger.CHAT_SUCCESS_SET_STYLE, "style", nameStyle.key());
                }
                break;
            case "message":
                ChatManager.MessageStyle messageStyle = chatManager.getMessageStyle(key);
                if (messageStyle == null){
                    messenger.send(sender, Messenger.CHAT_STYLE_NOT_EXIST);
                }else if (!target.isOnline() || target.getPlayer() == null){
                    messenger.send(sender, Messenger.PLAYER_NOT_ONLINE);
                }else if (!target.getPlayer().hasPermission(messageStyle.permission())){
                    messenger.send(sender, Messenger.CHAT_STYLE_NOT_PERMISSION, "permission", messageStyle.permission());
                }else {
                    chatData.setMessageStyle(messageStyle.key());
                    messenger.send(sender, Messenger.CHAT_SUCCESS_SET_STYLE, "style", messageStyle.key());
                }
                break;
            case "chat":
                ChatManager.ChatStyle chatStyle = chatManager.getChatStyle(key);
                if (chatStyle == null){
                    messenger.send(sender, Messenger.CHAT_STYLE_NOT_EXIST);
                }else if (!target.isOnline() || target.getPlayer() == null){
                    messenger.send(sender, Messenger.PLAYER_NOT_ONLINE);
                }else if (!target.getPlayer().hasPermission(chatStyle.permission())){
                    messenger.send(sender, Messenger.CHAT_STYLE_NOT_PERMISSION, "permission", chatStyle.permission());
                }else {
                    chatData.setChatStyle(chatStyle.key());
                    messenger.send(sender, Messenger.CHAT_SUCCESS_SET_STYLE, "style", chatStyle.key());
                }
                break;
            case "sound":
                ChatManager.SoundStyle soundStyle = chatManager.getSoundStyle(key);
                if (soundStyle == null){
                    messenger.send(sender, Messenger.CHAT_STYLE_NOT_EXIST);
                }else if (!target.isOnline() || target.getPlayer() == null){
                    messenger.send(sender, Messenger.PLAYER_NOT_ONLINE);
                }else if (!target.getPlayer().hasPermission(soundStyle.permission())){
                    messenger.send(sender, Messenger.CHAT_STYLE_NOT_PERMISSION, "permission", soundStyle.permission());
                }else {
                    chatData.setSoundStyle(soundStyle.getKey());
                    messenger.send(sender, Messenger.CHAT_SUCCESS_SET_STYLE, "style", soundStyle.getKey());
                }
                break;
            default:
                sendUsage(sender);
                break;
        }
    }

    public List<String> onPlayerTab(Player player, String[] args) {
        return onTab(player, args);
    }

    public List<String> onConsoleTab(CommandSender sender, String[] args) {
        return onTab(sender, args);
    }

    private List<String> onTab(CommandSender sender, String[] args) {
        if (args.length == 2){
            return TAB.entrySet().stream().filter(e -> sender.hasPermission(e.getValue()) && e.getKey().contains(args[1])).map(Map.Entry::getKey).toList();
        }
        if (args.length == 3){
            return TAB2;
        }
        if (args.length == 4){
            String perm = TAB.get(args[1].toLowerCase());
            if (perm == null || !sender.hasPermission(perm)){
                return List.of();
            }
            return switch (args[1].toLowerCase()){
                case "name" -> chatManager.getNameStyles().entrySet().stream()
                        .filter(e -> (e.getValue().permission() == null || sender.hasPermission(e.getValue().permission())) && e.getKey().contains(args[3]))
                        .map(Map.Entry::getKey)
                        .toList();
                case "message" -> chatManager.getMessageStyles().entrySet().stream()
                        .filter(e -> (e.getValue().permission() == null || sender.hasPermission(e.getValue().permission())) && e.getKey().contains(args[3]))
                        .map(Map.Entry::getKey)
                        .toList();
                case "chat" -> chatManager.getChatStyles().entrySet().stream()
                        .filter(e -> (e.getValue().permission() == null || sender.hasPermission(e.getValue().permission())) && e.getKey().contains(args[3]))
                        .map(Map.Entry::getKey)
                        .toList();
                case "sound" -> chatManager.getSoundStyles().entrySet().stream()
                        .filter(e -> (e.getValue().permission() == null || sender.hasPermission(e.getValue().permission())) && e.getKey().contains(args[3]))
                        .map(Map.Entry::getKey)
                        .toList();
                default -> List.of();
            };
        }
        if (args.length == 5){
            return PlayerUtil.getAllPlayerNames(args[4]);
        }
        return List.of();
    }
}
