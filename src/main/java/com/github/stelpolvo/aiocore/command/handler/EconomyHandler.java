package com.github.stelpolvo.aiocore.command.handler;

import com.github.stelpolvo.aiocore.api.EconomyManager;
import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.api.PlayerDataManager;
import com.github.stelpolvo.aiocore.api.data.EconomyData;
import com.github.stelpolvo.aiocore.api.data.PlayerData;
import com.github.stelpolvo.aiocore.command.CommandAPI;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * /aio economy pay [playerName] [currency] [amount]
 * /aio economy look [currency]
 * /aio economy get [playerName] [currency]
 * /aio economy set [playerName] [currency] [amount]
 * /aio economy take [playerName] [currency] [amount]
 * /aio economy give [playerName] [currency] [amount]
 */
public class EconomyHandler implements CommandAPI {
    public static final Map<String, String> PERM_MAP = ImmutableMap.of(
            "pay", "aio.def.economy.pay",
            "look", "aio.def.economy.look",
            "get", "aio.admin.economy.get",
            "set", "aio.admin.economy.set",
            "take", "aio.admin.economy.take",
            "give", "aio.admin.economy.give"
    );

    public static final List<String> AMOUNT_LIST = ImmutableList.of("1", "10", "100", "1000", "10000");

    private final EconomyManager economy;
    private final PlayerDataManager manager;
    private final Messenger messenger;
    private final Logger logger;

    public EconomyHandler(EconomyManager economy, PlayerDataManager manager, Messenger messenger, Logger logger) {
        this.economy = economy;
        this.manager = manager;
        this.messenger = messenger;
        this.logger = logger;
    }

    @Override
    public boolean onPlayer(Player player, String[] args) {
        return onCommand(player, args);
    }

    @Override
    public boolean onConsole(CommandSender sender, String[] args) {
        return onCommand(sender, args);
    }

    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length >= 3){
            String rootArg = args[1].toLowerCase(Locale.ROOT);
            if (!(sender instanceof Player)){
                if (rootArg.equals("pay") || rootArg.equals("look")){
                    messenger.send(sender, Messenger.IS_PLAYER_COMMAND);
                    return true;
                }
            }
            try {
                return switch (rootArg) {
                    case "pay" ->
                            sender.hasPermission(PERM_MAP.get(rootArg)) ? pay((Player) sender, args) : messenger.send(sender, Messenger.NO_PERMISSION, "permission", PERM_MAP.get(rootArg));
                    case "look" ->
                            sender.hasPermission(PERM_MAP.get(rootArg)) ? look((Player) sender, args) : messenger.send(sender, Messenger.NO_PERMISSION, "permission", PERM_MAP.get(rootArg));
                    case "get" ->
                            sender.hasPermission(PERM_MAP.get(rootArg)) ? get(sender, args) : messenger.send(sender, Messenger.NO_PERMISSION, "permission", PERM_MAP.get(rootArg));
                    case "set" ->
                            sender.hasPermission(PERM_MAP.get(rootArg)) ? set(sender, args) : messenger.send(sender, Messenger.NO_PERMISSION, "permission", PERM_MAP.get(rootArg));
                    case "take" ->
                            sender.hasPermission(PERM_MAP.get(rootArg)) ? take(sender, args) : messenger.send(sender, Messenger.NO_PERMISSION, "permission", PERM_MAP.get(rootArg));
                    case "give" ->
                            sender.hasPermission(PERM_MAP.get(rootArg)) ? give(sender, args) : messenger.send(sender, Messenger.NO_PERMISSION, "permission", PERM_MAP.get(rootArg));
                    default -> sendUsage(sender);
                };
            }catch (NumberFormatException ne){
                messenger.send(sender, Messenger.NUMBER_FORMAT_EXCEPTION);
            }catch (Exception e){
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

        }
        return true;
    }

    public boolean pay(Player player, String[] args){
        if (args.length < 5){
            sendUsage(player);
            return true;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[3]);
        if (!offlinePlayer.hasPlayedBefore()){
            messenger.send(player, Messenger.ECONOMY_INVALID_RECEIVER);
            return true;
        }
        String currency = args[3];
        EconomyManager.AiOEconomy eco = economy.getAiOEconomy(currency);
        if (eco == null){
            messenger.send(player, Messenger.ECONOMY_INVALID_CURRENCY);
            return true;
        }
        double amount = Double.parseDouble(args[4]);
        if (amount < 0){
            messenger.send(player, Messenger.ECONOMY_AMOUNT_TOO_SMALL);
            return true;
        }
        EconomyData payerData = manager.getByUUID(player.getUniqueId()).getEconomyData();
        EconomyData receiverData = manager.getByUUID(offlinePlayer.getUniqueId()).getEconomyData();
        if (!(payerData.get(currency) < amount)) {
            messenger.send(player, Messenger.ECONOMY_INSUFFICIENT_FUNDS);
            return true;
        }
        if ((receiverData.get(currency) + amount) > Double.MAX_VALUE)  {
            messenger.send(player, Messenger.ECONOMY_AMOUNT_TOO_LARGE);
            return true;
        }
        if (eco.isTransferable()) {
            PlayerData sender = manager.getPlayerData().get(player.getUniqueId());
            PlayerData receiver = manager.getPlayerData().get(offlinePlayer.getUniqueId());
            sender.getEconomyData().set(args[3], sender.getEconomyData().get(args[3]) - amount);
            receiver.getEconomyData().set(args[3], receiver.getEconomyData().get(args[3]) + amount);
            messenger.send(player, Messenger.ECONOMY_SUCCESS_SENDER, "receiver", offlinePlayer.getName(), "amount", eco.getEconomy().format(amount), "currency", eco.getEconomy().currencyNamePlural());
            if (offlinePlayer.isOnline() && offlinePlayer instanceof Player r){
                messenger.send(r, Messenger.ECONOMY_SUCCESS_RECEIVER, "sender", r.getName(), "amount", eco.getEconomy().format(amount), "currency", eco.getEconomy().currencyNamePlural());
            }
        }else {
            messenger.send(player, Messenger.ECONOMY_DISABLED);
        }
        return true;
    }



    public OfflinePlayer getOfflinePlayer(String playerName){
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()){
            return null;
        }
        return offlinePlayer;
    }

    public boolean look(Player player, String[] args){
        EconomyManager.AiOEconomy eco = economy.getAiOEconomy(args[2]);
        if (eco == null){
            messenger.send(player, Messenger.ECONOMY_INVALID_CURRENCY);
            return true;
        }
        messenger.send(player, Messenger.ECONOMY_LOOK, "amount", eco.getEconomy().format(eco.getEconomy().getBalance(player)), "currency", eco.getEconomy().currencyNamePlural());
        return true;
    }
    public boolean get(CommandSender sender, String[] args){
        if (args.length < 4){
            sendUsage(sender);
            return true;
        }
        OfflinePlayer offlinePlayer = getOfflinePlayer(args[2]);
        if (offlinePlayer == null){
            messenger.send(sender, Messenger.ECONOMY_INVALID_RECEIVER);
            return true;
        }
        EconomyManager.AiOEconomy eco = economy.getAiOEconomy(args[3]);
        if (eco == null){
            messenger.send(sender, Messenger.ECONOMY_INVALID_CURRENCY);
            return true;
        }
        messenger.send(
                sender,
                Messenger.ECONOMY_GET,
                "player", offlinePlayer.getName(),
                "currency", eco.getEconomy().currencyNamePlural(),
                "amount", eco.getEconomy().format(eco.getEconomy().getBalance(offlinePlayer)));
        return true;
    }
    public boolean set(CommandSender sender, String[] args){
        if (args.length < 5){
            sendUsage(sender);
            return true;
        }
        OfflinePlayer offlinePlayer = getOfflinePlayer(args[2]);
        if (offlinePlayer == null){
            messenger.send(sender, Messenger.ECONOMY_INVALID_RECEIVER);
            return true;
        }
        EconomyManager.AiOEconomy eco = economy.getAiOEconomy(args[3]);
        if (eco == null){
            messenger.send(sender, Messenger.ECONOMY_INVALID_CURRENCY);
            return true;
        }
        EconomyData data = manager.getByName(offlinePlayer.getName()).getEconomyData();
        data.set(args[3], Double.parseDouble(args[4]));
        messenger.send(sender, Messenger.ECONOMY_SET, "player", offlinePlayer.getName(), "currency", eco.getEconomy().currencyNamePlural(), "amount", eco.getEconomy().format(data.get(args[3])));
        return true;
    }

    public boolean take(CommandSender sender, String[] args){
        if (args.length < 5){
            sendUsage(sender);
            return true;
        }
        OfflinePlayer offlinePlayer = getOfflinePlayer(args[2]);
        if (offlinePlayer == null){
            messenger.send(sender, Messenger.ECONOMY_INVALID_RECEIVER);
            return true;
        }
        EconomyManager.AiOEconomy eco = economy.getAiOEconomy(args[3]);
        if (eco == null){
            messenger.send(sender, Messenger.ECONOMY_INVALID_CURRENCY);
            return true;
        }
        EconomyData data = manager.getByName(offlinePlayer.getName()).getEconomyData();
        double take = Double.parseDouble(args[4]);
        double has = data.get(args[3]);
        if (take > has){
            take = has;
        }
        data.set(args[3], Math.max(0, data.get(args[3]) - take));
        messenger.send(sender, Messenger.ECONOMY_TAKE, "player", offlinePlayer.getName(), "currency", eco.getEconomy().currencyNamePlural(), "amount", eco.getEconomy().format(take));

        return true;
    }
    public boolean give(CommandSender sender, String[] args){
        if (args.length < 5){
            sendUsage(sender);
            return true;
        }
        OfflinePlayer offlinePlayer = getOfflinePlayer(args[2]);
        if (offlinePlayer == null){
            messenger.send(sender, Messenger.ECONOMY_INVALID_RECEIVER);
            return true;
        }
        EconomyManager.AiOEconomy eco = economy.getAiOEconomy(args[3]);
        if (eco == null){
            messenger.send(sender, Messenger.ECONOMY_INVALID_CURRENCY);
            return true;
        }
        EconomyData data = manager.getByName(offlinePlayer.getName()).getEconomyData();
        data.set(args[3], data.get(args[3]) + Double.parseDouble(args[4]));
        messenger.send(sender, Messenger.ECONOMY_GIVE, "player", offlinePlayer.getName(), "currency", eco.getEconomy().currencyNamePlural(), "amount", eco.getEconomy().format(data.get(args[3])));
        return true;
    }
    public boolean sendUsage(CommandSender sender){
        messenger.send(sender, "/aio economy pay [playerName] [currency] [amount]");
        messenger.send(sender, "/aio economy look [currency]");
        if (sender.isOp()){
            messenger.send(sender, "/aio economy get [playerName] [currency]");
            messenger.send(sender, "/aio economy set [playerName] [currency] [amount]");
            messenger.send(sender, "/aio economy take [playerName] [currency] [amount]");
            messenger.send(sender, "/aio economy give [playerName] [currency] [amount]");
        }
        return true;
    }

    public List<String> onPlayerTab(Player player, String[] args) {
        return onTabComplete(player, args);
    }

    public List<String> onConsoleTab(CommandSender sender, String[] args) {
        return onTabComplete(sender, args);
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2){
            return PERM_MAP.entrySet().stream().filter(e -> sender.hasPermission(e.getValue()) && sender instanceof Player && e.getKey().contains(args[1].toLowerCase())).map(Map.Entry::getKey).toList();
        }
        String rootArg = args[1].toLowerCase();
        if (args.length == 3){
            switch (rootArg) {
                case "look":
                    return economy.getCurrencyList();
                case "pay":
                    if (!(sender instanceof Player)){
                        return List.of();
                    }
                case "get", "set", "take", "give":
                    return sender.hasPermission(PERM_MAP.get(rootArg)) ? Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).filter(Objects::nonNull).filter(s -> s.contains(args[2])).toList() : List.of();
                default:
                    return List.of();
            }
        }
        return switch (rootArg) {
            case "pay", "get", "set", "take", "give" -> sender.hasPermission(PERM_MAP.get(rootArg)) ? (args.length == 4 ? economy.getCurrencyList() : (args.length == 5 ? AMOUNT_LIST : List.of())) : List.of();
            default -> List.of();
        };
    }

}