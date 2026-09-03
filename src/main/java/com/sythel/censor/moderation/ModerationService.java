package com.sythel.censor.moderation;

import com.sythel.censor.configuration.CensorConfiguration;

public final class ModerationService {

    private final FilterPipeline filterPipeline;

    public ModerationService(FilterPipeline filterPipeline) {
        this.filterPipeline = filterPipeline;
    }

    public CensorResult moderate(
            String message,
            CensorConfiguration configuration
    ) {
        return filterPipeline.process(
                message,
                configuration.replacement(),
                configuration
        );
    }
}