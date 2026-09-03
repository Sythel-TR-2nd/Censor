package com.sythel.censor.initialization;

import com.sythel.censor.Censor;
import com.sythel.censor.command.CensorCommand;
import com.sythel.censor.configuration.ConfigurationService;
import com.sythel.censor.listener.ChatListener;
import com.sythel.censor.logging.LogService;
import com.sythel.censor.logging.ModerationLogger;
import com.sythel.censor.message.MessageService;
import com.sythel.censor.message.StaffNotificationService;
import com.sythel.censor.moderation.AdvertisementFilter;
import com.sythel.censor.moderation.AdvertisementMatcher;
import com.sythel.censor.moderation.DomainValidator;
import com.sythel.censor.moderation.Filter;
import com.sythel.censor.moderation.FilterPipeline;
import com.sythel.censor.moderation.ModerationService;
import com.sythel.censor.moderation.UrlFilter;
import com.sythel.censor.moderation.UrlMatcher;
import com.sythel.censor.moderation.WordFilter;
import com.sythel.censor.moderation.WordMatcher;
import com.sythel.censor.moderation.WordTestService;
import com.sythel.censor.moderation.WordVariationGenerator;
import com.sythel.censor.service.CensorContext;
import com.sythel.censor.service.ReloadService;

import java.util.List;

public final class PluginInitializer {

    private final Censor plugin;

    public PluginInitializer(Censor plugin) {
        this.plugin = plugin;
    }

    public CensorContext initialize() {
        ConfigurationService configurationService =
                new ConfigurationService(plugin);

        configurationService.load();

        WordMatcher wordMatcher = new WordMatcher();

        wordMatcher.reload(
                configurationService.getConfiguration()
                        .blockedWords()
        );

        AdvertisementMatcher advertisementMatcher =
                new AdvertisementMatcher();

        advertisementMatcher.reload(
                configurationService.getConfiguration()
                        .advertisementPatterns()
        );

        ModerationLogger moderationLogger =
                new ModerationLogger(
                        plugin,
                        configurationService.getConfiguration()
                );

        ReloadService reloadService =
                new ReloadService(
                        configurationService,
                        wordMatcher,
                        advertisementMatcher,
                        moderationLogger
                );

        WordFilter wordFilter =
                new WordFilter(wordMatcher);

        DomainValidator domainValidator =
                new DomainValidator();

        UrlMatcher urlMatcher =
                new UrlMatcher(domainValidator);

        UrlFilter urlFilter =
                new UrlFilter(urlMatcher);

        AdvertisementFilter advertisementFilter =
                new AdvertisementFilter(
                        advertisementMatcher
                );

        List<Filter> filters = List.of(
                wordFilter,
                urlFilter,
                advertisementFilter
        );

        FilterPipeline filterPipeline =
                new FilterPipeline(filters);

        ModerationService moderationService =
                new ModerationService(
                        filterPipeline
                );

        MessageService messageService =
                new MessageService();

        StaffNotificationService staffNotificationService =
                new StaffNotificationService(
                        plugin
                );

        LogService logService =
                new LogService(
                        moderationLogger
                );

        WordVariationGenerator variationGenerator =
                new WordVariationGenerator();

        WordTestService wordTestService =
                new WordTestService(
                        wordMatcher,
                        variationGenerator
                );

        CensorCommand censorCommand =
                new CensorCommand(
                        reloadService,
                        logService,
                        wordTestService
                );

        plugin.getCommand("censor")
                .setExecutor(censorCommand);

        plugin.getCommand("censor")
                .setTabCompleter(censorCommand);

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        new ChatListener(
                                moderationService,
                                configurationService,
                                staffNotificationService,
                                moderationLogger
                        ),
                        plugin
                );

        return new CensorContext(
                configurationService,
                moderationService,
                messageService,
                staffNotificationService,
                reloadService,
                moderationLogger
        );
    }
}