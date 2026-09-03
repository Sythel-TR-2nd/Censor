package com.sythel.censor.logging;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SqliteLogStorage implements LogStorage {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter ISO_TIME_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final DateTimeFormatter LEGACY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String JDBC_PREFIX =
            "jdbc:sqlite:";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS moderation_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                player TEXT NOT NULL,
                types TEXT NOT NULL,
                original_message TEXT NOT NULL,
                censored_message TEXT NOT NULL
            )
            """;

    private static final String CREATE_TIMESTAMP_INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS idx_moderation_logs_timestamp
            ON moderation_logs(timestamp DESC)
            """;

    private static final String INSERT_SQL = """
            INSERT INTO moderation_logs (
                timestamp,
                player,
                types,
                original_message,
                censored_message
            )
            SELECT ?, ?, ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1
                FROM moderation_logs
                WHERE timestamp = ?
                AND player = ?
                AND types = ?
                AND original_message = ?
                AND censored_message = ?
            )
            """;

    private static final String SELECT_SQL = """
            SELECT
                timestamp,
                player,
                types,
                original_message,
                censored_message
            FROM moderation_logs
            ORDER BY timestamp DESC, id DESC
            """;

    private final JavaPlugin plugin;
    private final Path logDirectory;
    private final Path legacyDatabase;

    public SqliteLogStorage(JavaPlugin plugin) {
        this.plugin = plugin;

        Path dataFolder =
                plugin.getDataFolder().toPath();

        this.logDirectory =
                dataFolder.resolve("logs");

        this.legacyDatabase =
                dataFolder.resolve("logs.db");

        createLogDirectory();
    }

    @Override
    public synchronized void save(LogEntry entry) {
        if (entry == null) {
            return;
        }

        Path database =
                getDatabasePath(entry);

        try (Connection connection =
                     openConnection(database)) {

            initializeTable(connection);

            String types =
                    String.join(
                            ",",
                            entry.types()
                    );

            String timestamp =
                    entry.timestamp()
                            .format(ISO_TIME_FORMAT);

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 INSERT_SQL
                         )) {

                statement.setString(1, timestamp);
                statement.setString(2, entry.playerName());
                statement.setString(3, types);
                statement.setString(4, entry.originalMessage());
                statement.setString(5, entry.censoredMessage());

                statement.setString(6, timestamp);
                statement.setString(7, entry.playerName());
                statement.setString(8, types);
                statement.setString(9, entry.originalMessage());
                statement.setString(10, entry.censoredMessage());

                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "SQLite moderasyon logu yazılamadı: "
                            + database
                            + " - "
                            + exception.getMessage()
            );
        }
    }

    @Override
    public synchronized List<LogEntry> findAll() {
        List<LogEntry> entries =
                new ArrayList<>();

        if (Files.exists(legacyDatabase)) {
            entries.addAll(
                    readDatabase(
                            legacyDatabase
                    )
            );
        }

        if (Files.exists(logDirectory)) {
            try (var files =
                         Files.list(logDirectory)) {

                files.filter(this::isDatabaseFile)
                        .sorted()
                        .forEach(
                                path ->
                                        entries.addAll(
                                                readDatabase(
                                                        path
                                                )
                                        )
                        );

            } catch (IOException exception) {
                plugin.getLogger().severe(
                        "SQLite moderasyon logları okunamadı: "
                                + exception.getMessage()
                );
            }
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

    private List<LogEntry> readDatabase(
            Path database
    ) {
        List<LogEntry> entries =
                new ArrayList<>();

        try (Connection connection =
                     openConnection(database)) {

            initializeTable(connection);

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 SELECT_SQL
                         );
                 ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    LocalDateTime timestamp =
                            parseTimestamp(
                                    resultSet.getString(
                                            "timestamp"
                                    )
                            );

                    if (timestamp == null) {
                        continue;
                    }

                    entries.add(
                            new LogEntry(
                                    timestamp,
                                    resultSet.getString(
                                            "player"
                                    ),
                                    parseTypes(
                                            resultSet.getString(
                                                    "types"
                                            )
                                    ),
                                    resultSet.getString(
                                            "original_message"
                                    ),
                                    resultSet.getString(
                                            "censored_message"
                                    )
                            )
                    );
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "SQLite moderasyon logu okunamadı: "
                            + database
                            + " - "
                            + exception.getMessage()
            );
        }

        return entries;
    }

    private Connection openConnection(
            Path database
    ) throws SQLException {

        Connection connection =
                DriverManager.getConnection(
                        JDBC_PREFIX + database
                );

        configureConnection(connection);

        return connection;
    }

    private void configureConnection(
            Connection connection
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "PRAGMA busy_timeout = 5000"
                     )) {
            statement.execute();
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "PRAGMA journal_mode = WAL"
                     )) {
            statement.execute();
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "PRAGMA synchronous = NORMAL"
                     )) {
            statement.execute();
        }
    }

    private void initializeTable(
            Connection connection
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             CREATE_TABLE_SQL
                     )) {

            statement.executeUpdate();
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             CREATE_TIMESTAMP_INDEX_SQL
                     )) {

            statement.executeUpdate();
        }
    }

    private Path getDatabasePath(
            LogEntry entry
    ) {
        return logDirectory.resolve(
                entry.timestamp()
                        .toLocalDate()
                        .format(DATE_FORMAT)
                        + ".db"
        );
    }

    private boolean isDatabaseFile(
            Path path
    ) {
        return Files.isRegularFile(path)
                && path.getFileName()
                        .toString()
                        .toLowerCase()
                        .endsWith(".db");
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

    private List<String> parseTypes(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(
                        value.split(",")
                )
                .map(String::trim)
                .filter(
                        type -> !type.isEmpty()
                )
                .toList();
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