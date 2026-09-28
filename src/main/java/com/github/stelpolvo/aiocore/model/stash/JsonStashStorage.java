package com.github.stelpolvo.aiocore.model.stash;

import com.github.stelpolvo.aiocore.api.storage.StashStorage;
import com.github.stelpolvo.aiocore.utils.FileUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonStashStorage implements StashStorage {

    private static final String KEY_PAGES = "pages";
    private static final String KEY_VERSION = "version";
    private static final int FORMAT_VERSION = 1;

    private final Gson gson = new Gson();
    private final Logger logger;
    private final File folder;
    private final Map<UUID, JsonObject> roots = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pending = new ConcurrentHashMap<>();

    public JsonStashStorage(File folder, Logger logger) {
        this.logger = logger;
        FileUtil.createIfNotExists(folder, true);
        this.folder = folder;
    }

    @Override
    public JsonObject load(UUID uuid, int page) {
        JsonObject root = root(uuid);
        JsonObject pages = pages(root);
        if (pages == null || !pages.has(String.valueOf(page))) {
            return null;
        }
        return pages.getAsJsonObject(String.valueOf(page));
    }

    @Override
    public boolean save(UUID uuid, int page, JsonObject data) {
        JsonObject pages = pages(root(uuid));
        if (pages == null) {
            return false;
        }
        pages.add(String.valueOf(page), data);
        pending.put(uuid, Boolean.TRUE);
        return true;
    }

    @Override
    public void flush() {
        for (UUID uuid : pending.keySet()) {
            if (pending.remove(uuid) == null) {
                continue;
            }
            JsonObject root = roots.get(uuid);
            if (root != null) {
                write(uuid, root);
            }
        }
    }

    public void unload(UUID uuid) {
        boolean dirty = pending.remove(uuid) != null;
        JsonObject root = roots.remove(uuid);
        if (dirty && root != null) {
            write(uuid, root);
        }
    }

    public boolean isLoaded(UUID uuid) {
        return roots.containsKey(uuid);
    }

    private JsonObject root(UUID uuid) {
        return roots.computeIfAbsent(uuid, this::read);
    }

    private JsonObject pages(@NotNull JsonObject root) {
        if (!root.has(KEY_PAGES) || !root.get(KEY_PAGES).isJsonObject()) {
            root.add(KEY_PAGES, new JsonObject());
        }
        return root.getAsJsonObject(KEY_PAGES);
    }

    private JsonObject read(UUID uuid) {
        File file = file(uuid);
        if (!file.isFile()) {
            return emptyRoot();
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) {
                logger.warning("Empty stash file for " + uuid + ", starting from a blank stash");
                return emptyRoot();
            }
            return root;
        } catch (IOException | RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to read stash file " + file.getAbsolutePath()
                    + " for " + uuid + ", starting from a blank stash", e);
            return emptyRoot();
        }
    }

    private void write(UUID uuid, JsonObject root) {
        File file = file(uuid);
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
            writer.flush();
        } catch (IOException | RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to write stash file " + file.getAbsolutePath()
                    + " for " + uuid, e);
            return;
        }
        try {
            Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to replace stash file " + file.getAbsolutePath()
                    + " for " + uuid, e);
        }
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".json");
    }

    private static JsonObject emptyRoot() {
        JsonObject root = new JsonObject();
        root.addProperty(KEY_VERSION, FORMAT_VERSION);
        root.add(KEY_PAGES, new JsonObject());
        return root;
    }
}
