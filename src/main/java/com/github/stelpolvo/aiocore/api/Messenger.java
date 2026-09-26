package com.github.stelpolvo.aiocore.api;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

public interface Messenger {
    String PREFIX = "prefix";
    String NO_PERMISSION = "no-permission";
    String SUBCOMMAND_NOT_FOUND = "subcommand-not-found";
    String NUMBER_FORMAT_EXCEPTION = "number-format-exception";
    String IS_PLAYER_COMMAND = "is-player-command";
    String SUCCESS_SAVE_DATA = "success-save-data";
    String SUCCESS_LOAD_ECONOMY = "success-load-economy";
    String PLAYER_NOT_EXIST = "player-not-exist";
    String PLAYER_NOT_ONLINE = "player-not-online";

    String ECONOMY_SUCCESS_SENDER        = "success-sender";
    String ECONOMY_SUCCESS_RECEIVER      = "success-receiver";
    String ECONOMY_DISABLED              = "disabled";
    String ECONOMY_INVALID_CURRENCY      = "invalid-currency";
    String ECONOMY_INVALID_RECEIVER      = "invalid-receiver";
    String ECONOMY_AMOUNT_TOO_SMALL      = "amount-too-small";
    String ECONOMY_AMOUNT_TOO_LARGE      = "amount-too-large";
    String ECONOMY_INSUFFICIENT_FUNDS    = "insufficient-funds";
    String ECONOMY_ENABLE_VAULT = "enable-vault";
    String ECONOMY_LOOK = "look";
    String ECONOMY_GET = "get";
    String ECONOMY_SET = "set";
    String ECONOMY_TAKE = "take";
    String ECONOMY_GIVE = "give";

    String CHAT_STYLE_NOT_EXIST = "style-not-exist";
    String CHAT_STYLE_NOT_PERMISSION = "style-not-permission";
    String CHAT_SUCCESS_SET_STYLE = "success-set-style";
    String CHAT_STYLE_DISPLAY = "style-display";
    String CHAT_STYLE_DISPLAY_LINE = "style-display-line";

    void send(CommandSender sender, String msg);
    boolean send(CommandSender sender, String key, Object... keyValues);
    void load(YamlConfiguration config);
}
