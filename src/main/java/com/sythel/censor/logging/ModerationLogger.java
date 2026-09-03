package com.sythel.censor.logging;

import com.sythel.censor.Censor;
import com.sythel.censor.configuration.CensorConfiguration;
import com.sythel.censor.moderation.CensorResult;
import com.sythel.censor.moderation.ModerationType;

import java.time.LocalDateTime;
import java.util.List;

public final class ModerationLogger {

    private final Censor plugin;
    private volatile LogStorage storage;
    private volatile boolean enabled;

    public ModerationLogger(
            Censor plugin,
            CensorConfiguration configuration
    ) {
        this.plugin = plugin;
        reload(configuration);
    }

    public synchronized void reload(
            CensorConfiguration configuration
    ) {
        LogStorage oldStorage = storage;

        if (!configuration.loggingEnabled()) {
            storage = null;
            enabled = false;

            if (oldStorage != null) {
                oldStorage.close();
            }

            return;
        }

        LogStorage newStorage =
                createStorage(configuration);

        if (oldStorage != null) {
            List<LogEntry> entries =
                    oldStorage.findAll();

            migrate(entries, newStorage);
            oldStorage.close();
        }

        storage = newStorage;
        enabled = true;
    }

    public void log(
            String playerName,
            String originalMessage,
            CensorResult result
    ) {
        if (!enabled || result == null || !result.censored()) {
            return;
        }

        LogEntry entry = new LogEntry(
                LocalDateTime.now(),
                playerName,
                result.moderationTypes().stream()
                        .map(this::formatType)
                        .distinct()
                        .toList(),
                originalMessage,
                result.message()
        );

        plugin.getServer()
                .getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {
                            LogStorage currentStorage = storage;

                            if (!enabled || currentStorage == null) {
                                return;
                            }

                            currentStorage.save(entry);
                        }
                );
    }

    public List<LogEntry> findAll() {
        LogStorage currentStorage = storage;

        if (!enabled || currentStorage == null) {
            return List.of();
        }

        return currentStorage.findAll();
    }

    public synchronized void close() {
        LogStorage currentStorage = storage;

        storage = null;
        enabled = false;

        if (currentStorage != null) {
            currentStorage.close();
        }
    }

    private void migrate(
            List<LogEntry> entries,
            LogStorage destination
    ) {
        for (LogEntry entry : entries) {
            destination.save(entry);
        }
    }

    private LogStorage createStorage(
            CensorConfiguration configuration
    ) {
        return switch (configuration.loggingStorage()) {
            case "sqlite" -> new SqliteLogStorage(plugin);
            case "yaml" -> new YamlLogStorage(plugin);
            default -> new YamlLogStorage(plugin);
        };
    }

    private String formatType(ModerationType type) {
        return switch (type) {
            case WORD -> "Kelime";
            case URL -> "URL";
            case ADVERTISEMENT -> "Reklam";
        };
    }
}