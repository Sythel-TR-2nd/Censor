package com.sythel.censor.message;

import net.kyori.adventure.text.Component;

public final class MessageFormatter {

    private MessageFormatter() {
    }

    public static Component censor(String message) {
        return Component.text(message);
    }

    public static Component staffNotification(
            String playerName,
            String originalMessage,
            String censoredMessage
    ) {
        return Component.text(
                "§c[Censor] §f" + playerName
                        + " §7mesajı sansürlendi: §f"
                        + censoredMessage
        );
    }
}