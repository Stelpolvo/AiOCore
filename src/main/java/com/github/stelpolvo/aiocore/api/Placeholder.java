package com.github.stelpolvo.aiocore.api;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public interface Placeholder {
    String onPlaceholderRequest(final OfflinePlayer player, @NotNull final String[] params);
    String getPlaceholderPrefix();
}
