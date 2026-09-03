package com.sythel.censor.message;

import net.kyori.adventure.text.Component;

public final class MessageService {

    public Component censor(String message) {
        return MessageFormatter.censor(message);
    }
}