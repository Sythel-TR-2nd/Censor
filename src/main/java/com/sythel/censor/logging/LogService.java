package com.sythel.censor.logging;

import java.util.List;

public final class LogService {

    private final ModerationLogger moderationLogger;

    public LogService(ModerationLogger moderationLogger) {
        this.moderationLogger = moderationLogger;
    }

    public List<LogEntry> findAll() {
        return moderationLogger.findAll();
    }

    public List<LogEntry> findByPlayer(String playerName) {
        return findAll().stream()
                .filter(entry -> entry.playerName()
                        .equalsIgnoreCase(playerName))
                .toList();
    }
}