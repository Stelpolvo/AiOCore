package com.github.stelpolvo.aiocore.command;

import com.github.stelpolvo.aiocore.api.AiO;
import com.github.stelpolvo.aiocore.api.Messenger;
import com.github.stelpolvo.aiocore.command.handler.EconomyHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MainCommand implements TabExecutor {
    private final Map<String, CommandAPI> commands = new HashMap<>();
    private AiO aio;
    public static void init(AiO instance){
        PluginCommand command = instance.getJavaPlugin().getCommand("sealevel");
        MainCommand main = new MainCommand();
        Objects.requireNonNull(command).setExecutor(main);
        command.setTabCompleter(main);
        main.aio = instance;
        main.commands.put("money", new EconomyHandler(instance.getEconomyManager(), instance.getMessenger()));
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) return true;
        CommandAPI api;
        if ((api = commands.get(args[0])) != null) {
            if (!api.hasPermission(sender)){
                aio.getMessenger().send(sender, Messenger.NO_PERMISSION, "permission", api.getPermission());
                return true;
            }
            if (sender instanceof Player) {
                return api.onPlayer((Player) sender, args);
            }else {
                return api.onConsole(sender, args);
            }
        }
        aio.getMessenger().send(sender, Messenger.SUBCOMMAND_NOT_FOUND, "command", args[0]);
        return true;
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length >= 2){
            CommandAPI api;
            if ((api = commands.get(args[0])) != null) {
                if (sender instanceof Player) {
                    return api.onPlayerTab((Player) sender, args);
                }else {
                    return api.onConsoleTab(sender, args);
                }
            }
        }else if (args.length == 1){
            final List<String> tab = new ArrayList<>();
            commands.forEach((k, v)-> {
                if (v.hasPermission(sender) && k.contains(args[0])){
                    tab.add(k);
                }
            });
            return tab;
        }
        return Collections.emptyList();
    }
}
