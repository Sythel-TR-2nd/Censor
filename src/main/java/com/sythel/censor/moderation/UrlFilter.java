package com.sythel.censor.moderation;

import com.sythel.censor.configuration.CensorConfiguration;

import java.util.List;

public final class UrlFilter implements Filter {

    private final UrlMatcher urlMatcher;

    public UrlFilter(UrlMatcher urlMatcher) {
        this.urlMatcher = urlMatcher;
    }

    @Override
    public CensorResult filter(
            String message,
            String replacement,
            CensorConfiguration configuration
    ) {
        if (!configuration.urlFilterEnabled()) {
            return new CensorResult(
                    message,
                    message,
                    false,
                    List.of(),
                    List.of()
            );
        }

        if (!urlMatcher.containsUrl(message)) {
            return new CensorResult(
                    message,
                    message,
                    false,
                    List.of(),
                    List.of()
            );
        }

        return new CensorResult(
                message,
                urlMatcher.censor(message, replacement),
                true,
                List.of(),
                List.of(ModerationType.URL)
        );
    }
}