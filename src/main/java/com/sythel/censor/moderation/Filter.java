package com.sythel.censor.moderation;

import com.sythel.censor.configuration.CensorConfiguration;

public interface Filter {

    CensorResult filter(
            String message,
            String replacement,
            CensorConfiguration configuration
    );
}