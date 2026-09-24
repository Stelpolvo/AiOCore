package com.github.stelpolvo.aiocore.command.handler;

import com.github.stelpolvo.aiocore.api.EconomyManager;
import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.command.CommandAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /aio economy pay [currency] [playerName] [amount]
 * /aio economy look [currency]
 */
public class EconomyHandler implements CommandAPI {

    public static final String PERM = "aio.def.economy";

    private final EconomyManager manager;
    private final Messenger messenger;

    public EconomyHandler(EconomyManager manager, Messenger messenger) {
        this.manager = manager;
        this.messenger = messenger;
    }

    @Override
    public boolean onPlayer(Player player, String[] args) {
        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "pay"  -> handlePay(player, args);
            case "look" -> handleLook(player, args);
            default     -> sendUsage(player);
        }
        return true;
    }

    @Override
    public boolean onConsole(CommandSender sender, String[] args) {
        return false;
    }

    private void handlePay(Player player, String[] args) {
        if (args.length < 5) {
            messenger.send(player, Messenger.ECONOMY_USAGE_PAY);
            return;
        }

        String currencyKey = args[2];
        String targetName  = args[3];
        String amountRaw   = args[4];

        EconomyManager.AiOEconomy aiOEconomy = manager.getAiOEconomy(currencyKey);
        if (aiOEconomy == null) {
            messenger.send(player, Messenger.ECONOMY_INVALID_CURRENCY,
                    "currency", currencyKey);
            return;
        }

        if (!aiOEconomy.isTransferable()) {
            messenger.send(player, Messenger.ECONOMY_DISABLED,
                    "currency", currencyKey);
            return;
        }

        Economy economy = aiOEconomy.getEconomy();
        String currencyName = economy.currencyNamePlural();

        // 目标玩家
        OfflinePlayer target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore()) {
                messenger.send(player, Messenger.ECONOMY_INVALID_RECEIVER,
                        "receiver", targetName);
                return;
            }
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            messenger.send(player, Messenger.ECONOMY_SELF_TRANSFER);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountRaw);
        } catch (NumberFormatException e) {
            messenger.send(player, Messenger.ECONOMY_INVALID_AMOUNT,
                    "amount", amountRaw);
            return;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            messenger.send(player, Messenger.ECONOMY_AMOUNT_TOO_SMALL,
                    "currency", currencyName);
            return;
        }

        double totalCost = amount;
        String receiverName = target.getName() != null ? target.getName() : targetName;

        synchronized (this) {
            double senderBalance = economy.getBalance(player.getName());
            if (senderBalance < totalCost) {
                messenger.send(player, Messenger.ECONOMY_INSUFFICIENT_FUNDS,
                        "amount",   economy.format(totalCost),
                        "currency", currencyName);
                return;
            }

            EconomyResponse withdraw = economy.withdrawPlayer(player.getName(), totalCost);
            if (!withdraw.transactionSuccess()) {
                messenger.send(player, Messenger.ECONOMY_WITHDRAW_FAILED,
                        "reason", String.valueOf(withdraw.errorMessage));
                return;
            }

            EconomyResponse deposit = economy.depositPlayer(receiverName, amount);
            if (!deposit.transactionSuccess()) {
                // 回滚
                economy.depositPlayer(player.getName(), totalCost);
                messenger.send(player, Messenger.ECONOMY_DEPOSIT_FAILED,
                        "reason", String.valueOf(deposit.errorMessage));
                return;
            }
        }

        // 成功消息
        messenger.send(player, Messenger.ECONOMY_SUCCESS_SENDER,
                "receiver", receiverName,
                "amount",   economy.format(amount),
                "currency", currencyName,
                "fee",      economy.format(0));

        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            if (targetPlayer != null) {
                messenger.send(targetPlayer, Messenger.ECONOMY_SUCCESS_RECEIVER,
                        "sender",   player.getName(),
                        "amount",   economy.format(amount),
                        "currency", currencyName);
            }
        }

        // TODO: logging.log-transfers
    }

    private void handleLook(Player player, String[] args) {
        List<String> currencyKeys = new ArrayList<>();

        if (args.length >= 3) {
            currencyKeys.add(args[2]);
        } else {
            currencyKeys.addAll(manager.getCurrencyList());
        }

        if (currencyKeys.isEmpty()) {
            messenger.send(player, Messenger.ECONOMY_NO_CURRENCIES);
            return;
        }

        for (String currencyKey : currencyKeys) {
            EconomyManager.AiOEconomy aiOEconomy = manager.getAiOEconomy(currencyKey);
            if (aiOEconomy == null) {
                messenger.send(player, Messenger.ECONOMY_INVALID_CURRENCY,
                        "currency", currencyKey);
                continue;
            }

            Economy economy = aiOEconomy.getEconomy();
            double balance = economy.getBalance(player.getName());

            // look 是「余额展示」消息，也可以做一个专门的 key: economy.messages.look
            messenger.send(player, "look",
                    "currency", economy.currencyNamePlural(),
                    "amount",   economy.format(balance));
        }
    }

    private void sendUsage(Player player) {
        messenger.send(player, Messenger.ECONOMY_USAGE_PAY);
        messenger.send(player, Messenger.ECONOMY_USAGE_LOOK);
    }

    // ---------------------------------------------------------------------
    // Tab
    // ---------------------------------------------------------------------

    @Override
    public List<String> onPlayerTab(Player player, String[] args) {
        if (args.length == 2) {
            return filter(List.of("pay", "look"), args[1]);
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "pay" -> {
                if (args.length == 3) {
                    return filter(manager.getCurrencyList(), args[2]);
                }
                if (args.length == 4) {
                    return filter(
                            Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .collect(Collectors.toList()),
                            args[3]);
                }
                if (args.length == 5) {
                    return filter(List.of("1", "10", "100", "1000"), args[4]);
                }
            }
            case "look" -> {
                if (args.length == 3) {
                    return filter(manager.getCurrencyList(), args[2]);
                }
            }
            default -> { }
        }
        return List.of();
    }

    public List<String> onConsoleTab(CommandSender sender, String[] args) {
        return List.of();
    }

    private List<String> filter(List<String> candidates, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return candidates;
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(PERM);
    }

    public String getPermission() {
        return PERM;
    }
}