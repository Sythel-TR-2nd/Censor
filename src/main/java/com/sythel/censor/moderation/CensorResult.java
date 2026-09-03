package com.sythel.censor.moderation;

import java.util.List;

public record CensorResult(
        String originalMessage,
        String message,
        boolean censored,
        List<String> matchedWords,
        List<ModerationType> moderationTypes
) {
}