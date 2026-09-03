package com.sythel.censor.moderation;

import com.sythel.censor.configuration.CensorConfiguration;

import java.util.List;

public final class AdvertisementFilter implements Filter {

    private final AdvertisementMatcher advertisementMatcher;

    public AdvertisementFilter(AdvertisementMatcher advertisementMatcher) {
        this.advertisementMatcher = advertisementMatcher;
    }

    @Override
    public CensorResult filter(
            String message,
            String replacement,
            CensorConfiguration configuration
    ) {
        if (!configuration.advertisementFilterEnabled()) {
            return new CensorResult(
                    message,
                    message,
                    false,
                    List.of(),
                    List.of()
            );
        }

        if (!advertisementMatcher.containsAdvertisement(message)) {
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
                advertisementMatcher.censor(message, replacement),
                true,
                List.of(),
                List.of(ModerationType.ADVERTISEMENT)
        );
    }
}