/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

/**
 * Append-only, size-rotating text log writer.
 *
 * <p>Writes are append-only and serialized on a per-instance lock. When the
 * active file reaches the configured size it is rotated to
 * {@code <name>-1.log}, with older copies shifted up ({@code -2}, {@code -3}…),
 * preserving the full history. Only the server ever writes through this class;
 * it is never exposed to the GUI.</p>
 */
public final class RotatingLogWriter {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path baseFile;
    private final String baseName;
    private final Object lock = new Object();

    public RotatingLogWriter(Path baseFile) {
        this.baseFile = baseFile;
        String fileName = baseFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        this.baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /** Appends one line, rotating beforehand if the file exceeds the size cap. */
    public void append(String line, int maxFileSizeMb) {
        synchronized (lock) {
            try {
                Files.createDirectories(baseFile.getParent());
                rotateIfNeeded((long) maxFileSizeMb * 1024L * 1024L);
                Files.writeString(baseFile, line + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException e) {
                LOGGER.error("[F-Kits] Failed to write log line to {}", baseFile, e);
            }
        }
    }

    private void rotateIfNeeded(long maxBytes) throws IOException {
        if (!Files.exists(baseFile)) {
            return;
        }
        if (Files.size(baseFile) < maxBytes) {
            return;
        }
        Path dir = baseFile.getParent();

        int highest = 0;
        while (Files.exists(dir.resolve(baseName + "-" + (highest + 1) + ".log"))) {
            highest++;
        }
        for (int i = highest; i >= 1; i--) {
            Files.move(dir.resolve(baseName + "-" + i + ".log"),
                    dir.resolve(baseName + "-" + (i + 1) + ".log"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(baseFile, dir.resolve(baseName + "-1.log"),
                StandardCopyOption.REPLACE_EXISTING);
    }
}
