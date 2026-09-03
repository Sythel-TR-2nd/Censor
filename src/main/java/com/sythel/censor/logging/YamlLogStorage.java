package com.sythel.censor.logging;

import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlLogStorage implements LogStorage {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter ISO_TIME_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final DateTimeFormatter LEGACY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final Path logDirectory;
    private final Yaml yaml;

    public YamlLogStorage(JavaPlugin plugin) {
        this.plugin = plugin;

        this.logDirectory =
                plugin.getDataFolder()
                        .toPath()
                        .resolve("logs");

        DumperOptions options =
                new DumperOptions();

        options.setDefaultFlowStyle(
                DumperOptions.FlowStyle.BLOCK
        );

        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setDefaultScalarStyle(
                DumperOptions.ScalarStyle.PLAIN
        );

        this.yaml = new Yaml(options);

        createLogDirectory();
    }

    @Override
    public synchronized void save(LogEntry entry) {
        if (entry == null) {
            return;
        }

        Path logFile =
                getLogFile(entry);

        List<LogEntry> entries =
                new ArrayList<>(
                        loadEntries(logFile)
                );

        if (containsEntry(entries, entry)) {
            return;
        }

        entries.add(entry);

        entries.sort(
                Comparator.comparing(
                        LogEntry::timestamp
                ).reversed()
        );

        List<Map<String, Object>> data =
                entries.stream()
                        .map(this::toMap)
                        .toList();

        try (Writer writer =
                     Files.newBufferedWriter(
                             logFile,
                             StandardCharsets.UTF_8,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING,
                             StandardOpenOption.WRITE
                     )) {

            yaml.dump(data, writer);

        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "YAML moderasyon logu yazılamadı: "
                            + logFile
                            + " - "
                            + exception.getMessage()
            );
        }
    }

    @Override
    public synchronized List<LogEntry> findAll() {
        if (!Files.isDirectory(logDirectory)) {
            return List.of();
        }

        List<LogEntry> entries =
                new ArrayList<>();

        try (var files =
                     Files.list(logDirectory)) {

            files.filter(this::isYamlFile)
                    .sorted()
                    .forEach(
                            path ->
                                    entries.addAll(
                                            loadEntries(path)
                                    )
                    );

        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "YAML moderasyon logları okunamadı: "
                            + exception.getMessage()
            );

            return List.of();
        }

        return entries.stream()
                .distinct()
                .sorted(
                        Comparator.comparing(
                                LogEntry::timestamp
                        ).reversed()
                )
                .toList();
    }

    @Override
    public void close() {
    }

    private List<LogEntry> loadEntries(
            Path logFile
    ) {
        if (!Files.isRegularFile(logFile)) {
            return List.of();
        }

        try {
            String content =
                    Files.readString(
                            logFile,
                            StandardCharsets.UTF_8
                    );

            if (content.isBlank()) {
                return List.of();
            }

            Object loaded =
                    yaml.load(content);

            if (!(loaded instanceof List<?> list)) {
                return List.of();
            }

            List<LogEntry> entries =
                    new ArrayList<>();

            for (Object object : list) {
                if (!(object instanceof Map<?, ?> rawMap)) {
                    continue;
                }

                Map<String, Object> data =
                        new LinkedHashMap<>();

                rawMap.forEach(
                        (key, value) ->
                                data.put(
                                        String.valueOf(key),
                                        value
                                )
                );

                LogEntry entry =
                        fromMap(data);

                if (entry != null) {
                    entries.add(entry);
                }
            }

            return List.copyOf(entries);

        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().severe(
                    "YAML moderasyon logu okunamadı: "
                            + logFile
                            + " - "
                            + exception.getMessage()
            );

            return List.of();
        }
    }

    private Map<String, Object> toMap(
            LogEntry entry
    ) {
        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "time",
                entry.timestamp()
                        .format(ISO_TIME_FORMAT)
        );

        data.put(
                "player",
                entry.playerName()
        );

        data.put(
                "types",
                entry.types()
        );

        data.put(
                "original",
                entry.originalMessage()
        );

        data.put(
                "censored",
                entry.censoredMessage()
        );

        return data;
    }

    private LogEntry fromMap(
            Map<String, Object> data
    ) {
        try {
            Object timeObject =
                    data.get("time");

            Object playerObject =
                    data.get("player");

            Object originalObject =
                    data.get("original");

            Object censoredObject =
                    data.get("censored");

            if (timeObject == null
                    || playerObject == null
                    || originalObject == null
                    || censoredObject == null) {
                return null;
            }

            String time =
                    String.valueOf(timeObject).trim();

            String player =
                    String.valueOf(playerObject).trim();

            String original =
                    String.valueOf(originalObject);

            String censored =
                    String.valueOf(censoredObject);

            LocalDateTime timestamp =
                    parseTimestamp(time);

            if (timestamp == null
                    || player.isBlank()
                    || original.isBlank()
                    || censored.isBlank()) {
                return null;
            }

            List<String> types =
                    parseTypes(
                            data.get("types")
                    );

            return new LogEntry(
                    timestamp,
                    player,
                    types,
                    original,
                    censored
            );

        } catch (RuntimeException exception) {
            return null;
        }
    }

    private List<String> parseTypes(
            Object value
    ) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(
                            type -> !type.isEmpty()
                    )
                    .distinct()
                    .toList();
        }

        String type =
                String.valueOf(value).trim();

        if (type.isEmpty()) {
            return List.of();
        }

        return List.of(type);
    }

    private LocalDateTime parseTimestamp(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(
                    value,
                    ISO_TIME_FORMAT
            );
        } catch (DateTimeParseException exception) {
            try {
                return LocalDateTime.parse(
                        value,
                        LEGACY_TIME_FORMAT
                );
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private boolean containsEntry(
            List<LogEntry> entries,
            LogEntry target
    ) {
        for (LogEntry entry : entries) {
            if (sameEntry(entry, target)) {
                return true;
            }
        }

        return false;
    }

    private boolean sameEntry(
            LogEntry first,
            LogEntry second
    ) {
        return first.timestamp()
                .equals(second.timestamp())
                && first.playerName()
                .equals(second.playerName())
                && first.types()
                .equals(second.types())
                && first.originalMessage()
                .equals(second.originalMessage())
                && first.censoredMessage()
                .equals(second.censoredMessage());
    }

    private Path getLogFile(
            LogEntry entry
    ) {
        return logDirectory.resolve(
                entry.timestamp()
                        .toLocalDate()
                        .format(DATE_FORMAT)
                        + ".yml"
        );
    }

    private boolean isYamlFile(
            Path path
    ) {
        return Files.isRegularFile(path)
                && path.getFileName()
                        .toString()
                        .toLowerCase()
                        .endsWith(".yml");
    }

    private void createLogDirectory() {
        try {
            Files.createDirectories(
                    logDirectory
            );
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Log klasörü oluşturulamadı: "
                            + exception.getMessage()
            );
        }
    }
}