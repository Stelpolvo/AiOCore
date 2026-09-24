package com.github.stelpolvo.aiocore.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public interface CommandAPI {

    boolean onPlayer(Player player, String[] args);

    boolean onConsole(CommandSender sender, String[] args);

    List<String> onPlayerTab(Player player, String[] args);

    List<String> onConsoleTab(CommandSender sender, String[] args);

}
