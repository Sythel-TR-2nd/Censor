package com.sythel.censor.message;

import com.sythel.censor.Censor;
import com.sythel.censor.configuration.CensorConfiguration;
import com.sythel.censor.moderation.CensorResult;
import com.sythel.censor.moderation.ModerationType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public final class StaffNotificationService {

    private final Censor plugin;

    public StaffNotificationService(Censor plugin) {
        this.plugin = plugin;
    }

    public void notify(
            Player player,
            String originalMessage,
            CensorResult result,
            CensorConfiguration configuration
    ) {
        if (!configuration.notifyStaff()) {
            return;
        }

        Bukkit.getScheduler().runTask(
                plugin,
                () -> sendNotifications(
                        player,
                        originalMessage,
                        result,
                        configuration
                )
        );
    }

    private void sendNotifications(
            Player player,
            String originalMessage,
            CensorResult result,
            CensorConfiguration configuration
    ) {
        String permission = configuration.staffPermission();

        List<String> types = result.moderationTypes().stream()
                .map(this::formatType)
                .distinct()
                .toList();

        String message = "§c[Censor] §f"
                + player.getName()
                + " §7mesajı sansürlendi. §8[§f"
                + String.join("§7, §f", types)
                + "§8]";

        String original = "§c[Censor] §7Orijinal: §f"
                + originalMessage;

        String censored = "§c[Censor] §7Sansürlü: §f"
                + result.message();

        Bukkit.getOnlinePlayers().stream()
                .filter(target -> target.hasPermission(permission))
                .forEach(target -> {
                    target.sendMessage(message);
                    target.sendMessage(original);
                    target.sendMessage(censored);
                });
    }

    private String formatType(ModerationType type) {
        return switch (type) {
            case WORD -> "Kelime";
            case URL -> "URL";
            case ADVERTISEMENT -> "Reklam";
        };
    }
}