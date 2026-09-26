package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.ChatManager;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class TextStyleImpl implements ChatManager.Style {
    private final String key;
    private final List<String> color;
    private final String permission;
    public TextStyleImpl(String key, List<String> colors, String permission) {
        this.key = key;
        this.color = colors;
        this.permission = permission;
    }
    public String key() {
        return key;
    }

    public String parse(OfflinePlayer player, String raw) {
        if (raw == null || raw.isEmpty() || color == null || color.isEmpty()) {
            return raw;
        }
        int[] codePoints = raw.codePoints().toArray();
        int length = codePoints.length;

        if (color.size() == 1) {
            return colorPrefix(color.get(0)) + raw;
        }
        int[][] stops = new int[color.size()][];
        for (int i = 0; i < color.size(); i++) {
            stops[i] = hexToRgb(color.get(i));
        }

        int segments = stops.length - 1;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            float global = length == 1 ? 0f : (float) i / (length - 1);
            float scaled = global * segments;
            int segIndex = Math.min((int) scaled, segments - 1);
            float t = scaled - segIndex;

            int[] from = stops[segIndex];
            int[] to = stops[segIndex + 1];

            int r = lerp(from[0], to[0], t);
            int g = lerp(from[1], to[1], t);
            int b = lerp(from[2], to[2], t);

            sb.append(rgbPrefix(r, g, b));
            sb.appendCodePoint(codePoints[i]);
        }

        return sb.toString();
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static int[] hexToRgb(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new int[]{r, g, b};
    }

    private static String rgbPrefix(int r, int g, int b) {
        return String.format("§x§%X§%X§%X§%X§%X§%X",
                (r >> 4) & 0xF, r & 0xF,
                (g >> 4) & 0xF, g & 0xF,
                (b >> 4) & 0xF, b & 0xF);
    }

    private static String colorPrefix(String hex) {
        int[] rgb = hexToRgb(hex);
        return rgbPrefix(rgb[0], rgb[1], rgb[2]);
    }

    public String permission() {
        return permission;
    }
}
