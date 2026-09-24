package com.github.stelpolvo.aiocore.model.message;

import com.github.stelpolvo.aiocore.api.Messenger;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MessengerImpl implements Messenger {

    private final Map<String, String> messages = new HashMap<>();

    public MessengerImpl(YamlConfiguration config) {
        load(config);
    }

    public void load(YamlConfiguration config) {
        messages.clear();
        if (config == null) {
            return;
        }
        ConfigurationSection settingsSec = config.getConfigurationSection("settings.messages");
        if (settingsSec != null) {
            settingsSec.getKeys(false).forEach(k -> {
                String value = settingsSec.getString(k);
                if (value != null) {
                    messages.put(k, value);
                }
            });
        }

        ConfigurationSection ecoSec = config.getConfigurationSection("economy.messages");
        if (ecoSec != null) {
            ecoSec.getKeys(false).forEach(k -> {
                String value = ecoSec.getString(k);
                if (value != null) {
                    messages.put(k, value);
                }
            });
        }
    }

    @Override
    public void send(CommandSender sender, String msg) {
        send(sender, msg, new Object[0]);
    }

    @Override
    public boolean send(CommandSender sender, String key, Object... keyValues) {
        if (sender == null || key == null) {
            return false;
        }

        String raw = messages.get(key);
        if (raw == null) {
            sender.sendMessage(ChatColor.RED + messages.get(Messenger.PREFIX)+key);
            return false;
        }

        String text = applyPlaceholders(raw, keyValues);

        if (sender instanceof Player player) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        sender.sendMessage(colorize(messages.get(Messenger.PREFIX)+text));
        return true;
    }


    private String applyPlaceholders(String raw, Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return raw;
        }
        if ((keyValues.length & 1) != 0) {
            throw new IllegalArgumentException(
                    "Placeholder parameters must be paired: key1, value1, key2, value2, ...");
        }

        String result = raw;
        for (int i = 0; i < keyValues.length; i += 2) {
            String k = String.valueOf(keyValues[i]);
            Object v = keyValues[i + 1];
            result = result.replace("{" + k + "}", v == null ? "" : String.valueOf(v));
        }
        return result;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}