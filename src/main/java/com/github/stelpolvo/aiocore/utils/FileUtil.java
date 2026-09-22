package com.github.stelpolvo.aiocore.utils;

import java.io.File;
import java.io.IOException;

public class FileUtil {
    public static File createIfNotExists(File file, boolean isDirectory) {
        if (file == null) {
            return null;
        }

        if (file.exists()) {
            return file;
        }

        try {
            if (isDirectory) {
                if (!file.mkdirs() && !file.isDirectory()) {
                    throw new IOException("Failed to create directory: " + file);
                }
            } else {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    if (!parent.mkdirs() && !parent.isDirectory()) {
                        throw new IOException("Failed to create parent directory: " + parent);
                    }
                }
                if (!file.createNewFile() && !file.isFile()) {
                    throw new IOException("Failed to create file: " + file);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create " + (isDirectory ? "directory" : "file") + ": " + file, e);
        }

        return file;
    }
}
