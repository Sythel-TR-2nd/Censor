package com.sythel.censor.service;

import com.sythel.censor.configuration.ConfigurationService;
import com.sythel.censor.logging.ModerationLogger;
import com.sythel.censor.message.MessageService;
import com.sythel.censor.message.StaffNotificationService;
import com.sythel.censor.moderation.ModerationService;

public final class CensorContext {

    private final ConfigurationService configurationService;
    private final ModerationService moderationService;
    private final MessageService messageService;
    private final StaffNotificationService staffNotificationService;
    private final ReloadService reloadService;
    private final ModerationLogger moderationLogger;

    public CensorContext(
            ConfigurationService configurationService,
            ModerationService moderationService,
            MessageService messageService,
            StaffNotificationService staffNotificationService,
            ReloadService reloadService,
            ModerationLogger moderationLogger
    ) {
        this.configurationService = configurationService;
        this.moderationService = moderationService;
        this.messageService = messageService;
        this.staffNotificationService = staffNotificationService;
        this.reloadService = reloadService;
        this.moderationLogger = moderationLogger;
    }

    public ConfigurationService configurationService() {
        return configurationService;
    }

    public ModerationService moderationService() {
        return moderationService;
    }

    public MessageService messageService() {
        return messageService;
    }

    public StaffNotificationService staffNotificationService() {
        return staffNotificationService;
    }

    public ReloadService reloadService() {
        return reloadService;
    }

    public ModerationLogger moderationLogger() {
        return moderationLogger;
    }
}