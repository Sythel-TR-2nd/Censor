package com.sythel.censor.service;

import com.sythel.censor.configuration.ConfigurationService;
import com.sythel.censor.logging.ModerationLogger;
import com.sythel.censor.moderation.AdvertisementMatcher;
import com.sythel.censor.moderation.WordMatcher;

public final class ReloadService {

    private final ConfigurationService configurationService;
    private final WordMatcher wordMatcher;
    private final AdvertisementMatcher advertisementMatcher;
    private final ModerationLogger moderationLogger;

    public ReloadService(
            ConfigurationService configurationService,
            WordMatcher wordMatcher,
            AdvertisementMatcher advertisementMatcher,
            ModerationLogger moderationLogger
    ) {
        this.configurationService = configurationService;
        this.wordMatcher = wordMatcher;
        this.advertisementMatcher = advertisementMatcher;
        this.moderationLogger = moderationLogger;
    }

    public void reload() {
        configurationService.load();

        wordMatcher.reload(
                configurationService.getConfiguration().blockedWords()
        );

        advertisementMatcher.reload(
                configurationService.getConfiguration().advertisementPatterns()
        );

        moderationLogger.reload(
                configurationService.getConfiguration()
        );
    }
}