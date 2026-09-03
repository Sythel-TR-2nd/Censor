package com.sythel.censor.moderation;

public final class DomainValidator {

    public boolean isValid(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }

        String normalized = domain.trim().toLowerCase();

        if (normalized.length() > 253) {
            return false;
        }

        String[] labels = normalized.split("\\.", -1);

        if (labels.length < 2) {
            return false;
        }

        for (String label : labels) {
            if (!isValidLabel(label)) {
                return false;
            }
        }

        String tld = labels[labels.length - 1];

        return tld.length() >= 2
                && containsLetter(tld);
    }

    private boolean isValidLabel(String label) {
        if (label.isEmpty()
                || label.length() > 63
                || label.startsWith("-")
                || label.endsWith("-")) {
            return false;
        }

        for (int i = 0; i < label.length(); i++) {
            char character = label.charAt(i);

            if ((character < 'a' || character > 'z')
                    && (character < '0' || character > '9')
                    && character != '-') {
                return false;
            }
        }

        return true;
    }

    private boolean containsLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (character >= 'a' && character <= 'z') {
                return true;
            }
        }

        return false;
    }
}