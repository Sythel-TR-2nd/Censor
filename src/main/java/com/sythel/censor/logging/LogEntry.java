package com.sythel.censor.logging;

import java.time.LocalDateTime;
import java.util.List;

public record LogEntry(
        LocalDateTime timestamp,
        String playerName,
        List<String> types,
        String originalMessage,
        String censoredMessage
) {
}