package com.github.stelpolvo.aiocore.model.placeholder;

import com.github.stelpolvo.aiocore.api.AiO;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AiOHook extends PlaceholderExpansion {
    private final AiO aio;
    public AiOHook(AiO aio) {
        this.aio = aio;
    }
    public @NotNull String getIdentifier() {
        return AiO.NAME;
    }

    public @NotNull String getAuthor() {
        return aio.getJavaPlugin().getDescription().getAuthors().get(0);
    }

    public @NotNull String getVersion() {
        return aio.getJavaPlugin().getDescription().getVersion();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        String[] split = params.split("_");
        return switch (split[0]){
            case "economy" -> split.length >= 2 ? aio.getEconomyManager().onPlaceholderRequest(player, split) : "error";
            default -> throw new IllegalStateException("Unexpected value: " + split[0]);
        };
    }
}
