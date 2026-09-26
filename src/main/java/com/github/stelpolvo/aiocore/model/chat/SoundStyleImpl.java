package com.github.stelpolvo.aiocore.model.chat;

import com.github.stelpolvo.aiocore.api.ChatManager;
import org.bukkit.Sound;

public class SoundStyleImpl implements ChatManager.SoundStyle {
    private final String permission;
    private final String key;
    private Sound sound;
    private float volume = 1;
    private float pitch = 1;

    public SoundStyleImpl(String key, Sound sound, float volume, float pitch, String permission) {
        this.key = key;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.permission = permission;
    }

    public SoundStyleImpl(String key, String sound, String permission) {
        this.key = key;
        this.permission = permission;
        if (sound == null || sound.isEmpty()) {
            return;
        }
        String[] split = sound.split(";");
        if (split.length >= 3) {
            this.volume =  Float.parseFloat(split[1]);
            this.pitch = Float.parseFloat(split[2]);
        }
        this.sound = Sound.valueOf(split[0].toUpperCase());


    }

    public String getKey() {
        return key;
    }

    @Override
    public Sound getSound() {
        return this.sound;
    }

    public String permission() {
        return permission;
    }

    public float getPitch() {
        return pitch;
    }

    public float getVolume() {
        return volume;
    }
}