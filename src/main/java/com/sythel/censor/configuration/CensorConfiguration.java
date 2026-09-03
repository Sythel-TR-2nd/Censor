package com.sythel.censor.configuration;

import java.util.List;

public record CensorConfiguration(
        List<String> blockedWords,
        List<String> advertisementPatterns,
        String replacement,
        boolean notifyStaff,
        String staffPermission,
        boolean wordFilterEnabled,
        boolean urlFilterEnabled,
        boolean advertisementFilterEnabled,
        boolean loggingEnabled,
        String loggingStorage
) {
}