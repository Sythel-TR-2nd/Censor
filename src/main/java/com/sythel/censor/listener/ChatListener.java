package com.sythel.censor.listener;

import com.sythel.censor.configuration.ConfigurationService;
import com.sythel.censor.logging.ModerationLogger;
import com.sythel.censor.message.StaffNotificationService;
import com.sythel.censor.moderation.CensorResult;
import com.sythel.censor.moderation.ModerationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatListener implements Listener {

    private static final String BYPASS_PERMISSION = "censor.bypass";

    private final ModerationService moderationService;
    private final ConfigurationService configurationService;
    private final StaffNotificationService staffNotificationService;
    private final ModerationLogger moderationLogger;

    public ChatListener(
            ModerationService moderationService,
            ConfigurationService configurationService,
            StaffNotificationService staffNotificationService,
            ModerationLogger moderationLogger
    ) {
        this.moderationService = moderationService;
        this.configurationService = configurationService;
        this.staffNotificationService = staffNotificationService;
        this.moderationLogger = moderationLogger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        String message = event.getMessage();
        var configuration = configurationService.getConfiguration();

        CensorResult result = moderationService.moderate(
                message,
                configuration
        );

        if (!result.censored()) {
            return;
        }

        event.setMessage(result.message());

        moderationLogger.log(
                player.getName(),
                message,
                result
        );

        staffNotificationService.notify(
                player,
                message,
                result,
                configuration
        );
    }
}