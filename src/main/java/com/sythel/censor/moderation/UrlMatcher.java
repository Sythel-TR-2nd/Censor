package com.sythel.censor.moderation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlMatcher {

    private static final String DOMAIN_LABEL =
            "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?";

    private static final Pattern URL_PATTERN =
            Pattern.compile(
                    "(?i)(?<![\\p{L}\\p{N}_])"
                            + "(?:(?:https?://)|(?:www\\.)+)?"
                            + DOMAIN_LABEL
                            + "(?:\\."
                            + DOMAIN_LABEL
                            + ")+"
                            + "(?::\\d{1,5})?"
                            + "(?:[/\\?#][^\\s<>()]*)?"
                            + "(?![\\p{L}\\p{N}_])"
            );

    private static final Pattern IP_PATTERN =
            Pattern.compile(
                    "(?i)(?<![\\p{L}\\p{N}_])"
                            + "(?:(?:https?://))?"
                            + "(?:\\d{1,3}\\.){3}"
                            + "\\d{1,3}"
                            + "(?::\\d{1,5})?"
                            + "(?:[/\\?#][^\\s<>()]*)?"
                            + "(?![\\p{L}\\p{N}_])"
            );

    private final DomainValidator domainValidator;

    public UrlMatcher(DomainValidator domainValidator) {
        this.domainValidator = domainValidator;
    }

    public boolean containsUrl(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        return containsValidMatch(
                message,
                URL_PATTERN
        ) || containsValidMatch(
                message,
                IP_PATTERN
        );
    }

    public String censor(
            String message,
            String replacement
    ) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String result =
                censorPattern(
                        message,
                        replacement,
                        URL_PATTERN
                );

        return censorPattern(
                result,
                replacement,
                IP_PATTERN
        );
    }

    private String censorPattern(
            String message,
            String replacement,
            Pattern pattern
    ) {
        Matcher matcher =
                pattern.matcher(message);

        StringBuffer result =
                new StringBuffer();

        while (matcher.find()) {
            String value =
                    matcher.group();

            if (isValidUrl(value)) {
                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(
                                replacement
                        )
                );
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private boolean containsValidMatch(
            String message,
            Pattern pattern
    ) {
        Matcher matcher =
                pattern.matcher(message);

        while (matcher.find()) {
            if (isValidUrl(matcher.group())) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidUrl(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized =
                value.trim().toLowerCase();

        if (IP_PATTERN.matcher(normalized).matches()) {
            return isValidIp(normalized);
        }

        normalized =
                removeScheme(normalized);

        normalized =
                removeWww(normalized);

        int pathStart =
                findPathStart(normalized);

        if (pathStart >= 0) {
            normalized =
                    normalized.substring(
                            0,
                            pathStart
                    );
        }

        int portStart =
                findPortStart(normalized);

        if (portStart >= 0) {
            String port =
                    normalized.substring(
                            portStart + 1
                    );

            if (!isValidPort(port)) {
                return false;
            }

            normalized =
                    normalized.substring(
                            0,
                            portStart
                    );
        }

        return domainValidator.isValid(
                normalized
        );
    }

    private String removeScheme(
            String value
    ) {
        if (value.startsWith("https://")) {
            return value.substring(8);
        }

        if (value.startsWith("http://")) {
            return value.substring(7);
        }

        return value;
    }

    private String removeWww(
            String value
    ) {
        if (value.startsWith("www.")) {
            return value.substring(4);
        }

        return value;
    }

    private boolean isValidIp(
            String value
    ) {
        String ip =
                removeScheme(value);

        int pathStart =
                findPathStart(ip);

        if (pathStart >= 0) {
            ip =
                    ip.substring(
                            0,
                            pathStart
                    );
        }

        int portStart =
                findPortStart(ip);

        if (portStart >= 0) {
            String port =
                    ip.substring(
                            portStart + 1
                    );

            if (!isValidPort(port)) {
                return false;
            }

            ip =
                    ip.substring(
                            0,
                            portStart
                    );
        }

        String[] parts =
                ip.split(
                        "\\.",
                        -1
                );

        if (parts.length != 4) {
            return false;
        }

        for (String part : parts) {
            if (part.isEmpty()
                    || part.length() > 3) {
                return false;
            }

            try {
                int number =
                        Integer.parseInt(part);

                if (number < 0
                        || number > 255) {
                    return false;
                }
            } catch (NumberFormatException exception) {
                return false;
            }
        }

        return true;
    }

    private int findPathStart(
            String value
    ) {
        int slash =
                value.indexOf('/');

        int question =
                value.indexOf('?');

        int hash =
                value.indexOf('#');

        int result =
                value.length();

        if (slash >= 0) {
            result =
                    Math.min(
                            result,
                            slash
                    );
        }

        if (question >= 0) {
            result =
                    Math.min(
                            result,
                            question
                    );
        }

        if (hash >= 0) {
            result =
                    Math.min(
                            result,
                            hash
                    );
        }

        return result == value.length()
                ? -1
                : result;
    }

    private int findPortStart(
            String value
    ) {
        int colon =
                value.lastIndexOf(':');

        if (colon < 0
                || colon == value.length() - 1) {
            return -1;
        }

        String port =
                value.substring(
                        colon + 1
                );

        for (int i = 0;
             i < port.length();
             i++) {

            if (!Character.isDigit(
                    port.charAt(i)
            )) {
                return -1;
            }
        }

        return colon;
    }

    private boolean isValidPort(
            String value
    ) {
        if (value == null
                || value.isEmpty()
                || value.length() > 5) {
            return false;
        }

        try {
            int port =
                    Integer.parseInt(value);

            return port >= 1
                    && port <= 65535;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}