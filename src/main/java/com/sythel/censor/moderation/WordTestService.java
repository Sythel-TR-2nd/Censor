package com.sythel.censor.moderation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WordTestService {

    private static final int DISPLAY_LIMIT = 200;

    private final WordMatcher wordMatcher;
    private final WordVariationGenerator variationGenerator;

    public WordTestService(
            WordMatcher wordMatcher,
            WordVariationGenerator variationGenerator
    ) {
        this.wordMatcher = wordMatcher;
        this.variationGenerator = variationGenerator;
    }

    public WordTestResult test(String word) {
        List<WordVariationGenerator.TestVariation> variations =
                variationGenerator.generate(word);

        if (variations.isEmpty()) {
            return new WordTestResult(
                    word,
                    Map.of(),
                    0,
                    0,
                    0
            );
        }

        Map<String, List<VariationResult>> allResults =
                new LinkedHashMap<>();

        int passed = 0;
        int failed = 0;

        for (WordVariationGenerator.TestVariation variation :
                variations) {

            boolean blocked =
                    wordMatcher.censor(
                            variation.value(),
                            "***"
                    ).censored();

            VariationResult result =
                    new VariationResult(
                            variation.value(),
                            blocked
                    );

            allResults.computeIfAbsent(
                    variation.category(),
                    ignored -> new ArrayList<>()
            ).add(result);

            if (blocked) {
                passed++;
            } else {
                failed++;
            }
        }

        Map<String, List<VariationResult>> displayedResults =
                createRandomSample(allResults);

        return new WordTestResult(
                word,
                displayedResults,
                passed,
                failed,
                variations.size()
        );
    }

    public boolean isBlockedWord(String word) {
        if (word == null || word.isBlank()) {
            return false;
        }

        return wordMatcher.censor(
                word,
                "***"
        ).censored();
    }

    private Map<String, List<VariationResult>> createRandomSample(
            Map<String, List<VariationResult>> allResults
    ) {
        Map<String, List<VariationResult>> shuffledGroups =
                new LinkedHashMap<>();

        for (Map.Entry<String, List<VariationResult>> entry :
                allResults.entrySet()) {

            List<VariationResult> shuffled =
                    new ArrayList<>(
                            entry.getValue()
                    );

            Collections.shuffle(shuffled);

            shuffledGroups.put(
                    entry.getKey(),
                    shuffled
            );
        }

        Map<String, Integer> positions =
                new LinkedHashMap<>();

        for (String category : shuffledGroups.keySet()) {
            positions.put(category, 0);
        }

        Map<String, List<VariationResult>> result =
                new LinkedHashMap<>();

        int selected = 0;

        while (selected < DISPLAY_LIMIT) {
            boolean added = false;

            for (Map.Entry<String, List<VariationResult>> entry :
                    shuffledGroups.entrySet()) {

                if (selected >= DISPLAY_LIMIT) {
                    break;
                }

                String category =
                        entry.getKey();

                List<VariationResult> variations =
                        entry.getValue();

                int position =
                        positions.get(category);

                if (position >= variations.size()) {
                    continue;
                }

                result.computeIfAbsent(
                        category,
                        ignored -> new ArrayList<>()
                ).add(
                        variations.get(position)
                );

                positions.put(
                        category,
                        position + 1
                );

                selected++;
                added = true;
            }

            if (!added) {
                break;
            }
        }

        return result;
    }

    public record WordTestResult(
            String word,
            Map<String, List<VariationResult>> results,
            int passed,
            int failed,
            int totalVariations
    ) {

        public int total() {
            return passed + failed;
        }

        public int displayed() {
            return results.values()
                    .stream()
                    .mapToInt(List::size)
                    .sum();
        }

        public double percentage() {
            if (total() == 0) {
                return 100.0;
            }

            return passed * 100.0 / total();
        }
    }

    public record VariationResult(
            String value,
            boolean blocked
    ) {
    }
}