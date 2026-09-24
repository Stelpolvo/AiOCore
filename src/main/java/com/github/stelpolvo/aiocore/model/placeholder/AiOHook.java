package com.github.stelpolvo.aiocore.model.placeholder;

import com.github.stelpolvo.aiocore.api.AiO;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.jetbrains.annotations.NotNull;

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


}
