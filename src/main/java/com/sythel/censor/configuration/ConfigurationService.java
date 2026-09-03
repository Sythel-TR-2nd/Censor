package com.sythel.censor.configuration;

import com.sythel.censor.Censor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public final class ConfigurationService {

    private static final String DEFAULT_REPLACEMENT = "***";
    private static final String DEFAULT_STAFF_PERMISSION = "censor.notify";
    private static final String DEFAULT_LOGGING_STORAGE = "yaml";

    private final Censor plugin;
    private volatile CensorConfiguration configuration;

    public ConfigurationService(Censor plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        FileConfiguration censoredConfig = loadCensoredConfig();

        List<String> blockedWords = censoredConfig
                .getStringList("words")
                .stream()
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .distinct()
                .toList();

        List<String> advertisementPatterns = censoredConfig
                .getStringList("advertisements")
                .stream()
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .distinct()
                .toList();

        String replacement = config.getString(
                "replacement",
                DEFAULT_REPLACEMENT
        );

        if (replacement == null || replacement.isEmpty()) {
            replacement = DEFAULT_REPLACEMENT;
        }

        String staffPermission = config.getString(
                "staff-permission",
                DEFAULT_STAFF_PERMISSION
        );

        if (staffPermission == null || staffPermission.isBlank()) {
            staffPermission = DEFAULT_STAFF_PERMISSION;
        }

        String loggingStorage = config.getString(
                "logging.storage",
                DEFAULT_LOGGING_STORAGE
        );

        if (loggingStorage == null
                || (!loggingStorage.equalsIgnoreCase("yaml")
                && !loggingStorage.equalsIgnoreCase("sqlite"))) {
            loggingStorage = DEFAULT_LOGGING_STORAGE;
        }

        boolean wordFilterEnabled =
                config.getBoolean(
                        "filters.words.enabled",
                        true
                );

        boolean urlFilterEnabled =
                config.getBoolean(
                        "filters.urls.enabled",
                        true
                );

        boolean advertisementFilterEnabled =
                config.getBoolean(
                        "filters.advertisements.enabled",
                        true
                );

        boolean notifyStaff =
                config.getBoolean(
                        "notify-staff",
                        true
                );

        boolean loggingEnabled =
                config.getBoolean(
                        "logging.enabled",
                        true
                );

        configuration = new CensorConfiguration(
                blockedWords,
                advertisementPatterns,
                replacement,
                notifyStaff,
                staffPermission,
                wordFilterEnabled,
                urlFilterEnabled,
                advertisementFilterEnabled,
                loggingEnabled,
                loggingStorage.toLowerCase()
        );

        plugin.getLogger().info(
                "censored.yml yüklendi: "
                        + blockedWords.size()
                        + " kelime, "
                        + advertisementPatterns.size()
                        + " reklam kalıbı."
        );

        plugin.getLogger().info(
                "censored.yml konumu: "
                        + new File(
                                plugin.getDataFolder(),
                                "censored.yml"
                        ).getAbsolutePath()
        );
    }

    public CensorConfiguration getConfiguration() {
        return configuration;
    }

    private FileConfiguration loadCensoredConfig() {
        File file = new File(
                plugin.getDataFolder(),
                "censored.yml"
        );

        if (!file.exists()) {
            plugin.saveResource(
                    "censored.yml",
                    false
            );
        }

        return YamlConfiguration.loadConfiguration(file);
    }
}