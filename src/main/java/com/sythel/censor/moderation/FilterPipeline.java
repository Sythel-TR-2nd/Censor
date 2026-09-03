package com.sythel.censor.moderation;

import com.sythel.censor.configuration.CensorConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class FilterPipeline {

    private final List<Filter> filters;

    public FilterPipeline(List<Filter> filters) {
        this.filters = List.copyOf(filters);
    }

    public CensorResult process(
            String message,
            String replacement,
            CensorConfiguration configuration
    ) {
        String originalMessage = message;
        String currentMessage = message;

        List<String> matchedWords = new ArrayList<>();
        List<ModerationType> moderationTypes = new ArrayList<>();

        for (Filter filter : filters) {
            CensorResult result = filter.filter(
                    currentMessage,
                    replacement,
                    configuration
            );

            currentMessage = result.message();
            matchedWords.addAll(result.matchedWords());
            moderationTypes.addAll(result.moderationTypes());
        }

        return new CensorResult(
                originalMessage,
                currentMessage,
                !moderationTypes.isEmpty(),
                List.copyOf(matchedWords),
                List.copyOf(moderationTypes)
        );
    }
}