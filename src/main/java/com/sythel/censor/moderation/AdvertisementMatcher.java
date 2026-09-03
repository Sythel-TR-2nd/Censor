package com.sythel.censor.moderation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdvertisementMatcher {

    private volatile List<AdvertisementPattern> patterns = List.of();

    public void reload(List<String> advertisementPatterns) {
        List<AdvertisementPattern> newPatterns =
                advertisementPatterns.stream()
                        .filter(pattern ->
                                pattern != null && !pattern.isBlank()
                        )
                        .map(this::createPattern)
                        .toList();

        patterns = newPatterns;
    }

    public boolean containsAdvertisement(String message) {
        return patterns.stream()
                .anyMatch(pattern ->
                        pattern.pattern().matcher(message).find()
                );
    }

    public String censor(
            String message,
            String replacement
    ) {
        String result = message;
        String safeReplacement =
                Matcher.quoteReplacement(replacement);

        for (AdvertisementPattern advertisementPattern : patterns) {
            result = advertisementPattern.pattern()
                    .matcher(result)
                    .replaceAll(safeReplacement);
        }

        return result;
    }

    private AdvertisementPattern createPattern(
            String advertisementPattern
    ) {
        StringBuilder regex = new StringBuilder();

        for (int i = 0; i < advertisementPattern.length(); i++) {
            char character = advertisementPattern.charAt(i);

            if (Character.isWhitespace(character)) {
                regex.append("[\\s._-]+");
                continue;
            }

            if (isSeparator(character)) {
                regex.append("[\\s._-]*");
                continue;
            }

            regex.append(Pattern.quote(
                    String.valueOf(character)
            ));
        }

        Pattern pattern = Pattern.compile(
                "(?<![\\p{L}\\p{N}_])"
                        + regex
                        + "(?![\\p{L}\\p{N}_])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );

        return new AdvertisementPattern(
                advertisementPattern,
                pattern
        );
    }

    private boolean isSeparator(char character) {
        return character == '.'
                || character == ','
                || character == '_'
                || character == '-'
                || character == '|'
                || character == '/';
    }

    private record AdvertisementPattern(
            String value,
            Pattern pattern
    ) {
    }
}