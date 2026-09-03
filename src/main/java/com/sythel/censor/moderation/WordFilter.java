package com.sythel.censor.moderation;

import com.sythel.censor.configuration.CensorConfiguration;

import java.util.List;

public final class WordFilter implements Filter {

    private final WordMatcher wordMatcher;

    public WordFilter(WordMatcher wordMatcher) {
        this.wordMatcher = wordMatcher;
    }

    @Override
    public CensorResult filter(
            String message,
            String replacement,
            CensorConfiguration configuration
    ) {
        if (!configuration.wordFilterEnabled()) {
            return new CensorResult(
                    message,
                    message,
                    false,
                    List.of(),
                    List.of()
            );
        }

        return wordMatcher.censor(
                message,
                replacement
        );
    }
}