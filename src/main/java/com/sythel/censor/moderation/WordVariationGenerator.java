package com.sythel.censor.moderation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WordVariationGenerator {

    private static final char[] SEPARATORS = {
            ' ',
            '.',
            '-',
            '_',
            '!',
            '/',
            '|',
            '~',
            ':',
            ';',
            ','
    };

    private static final String[] MULTI_SEPARATORS = {
            "--",
            "---",
            "----",
            "__",
            "___",
            "____",
            "..",
            "...",
            "....",
            "!!",
            "!!!",
            "//",
            "///",
            "||",
            "|||",
            "~-~",
            "_-_",
            ".-.",
            "-.-",
            "_.-",
            "-_."
    };

    private static final int[] REPEAT_COUNTS = {
            2,
            3,
            4,
            5,
            8,
            12
    };

    private static final char[] INVISIBLE_CHARACTERS = {
            '\u200B',
            '\u200C',
            '\u200D',
            '\u2060',
            '\uFEFF',
            '\u034F',
            '\u061C'
    };

    private static final char[] COMBINING_MARKS = {
            '\u0300',
            '\u0301',
            '\u0302',
            '\u0303',
            '\u0304',
            '\u0307',
            '\u0308',
            '\u030A',
            '\u0310',
            '\u0315',
            '\u031B',
            '\u0323',
            '\u0324',
            '\u0327',
            '\u0328',
            '\u0334',
            '\u0335',
            '\u0336',
            '\u0337',
            '\u0338'
    };

    private static final Map<Character, char[]> CONFUSABLES =
            Map.ofEntries(
                    Map.entry('a', new char[]{'α', 'а'}),
                    Map.entry('b', new char[]{'β', 'в'}),
                    Map.entry('c', new char[]{'ϲ', 'с'}),
                    Map.entry('d', new char[]{'δ', 'д'}),
                    Map.entry('e', new char[]{'ε', 'е'}),
                    Map.entry('f', new char[]{'ф'}),
                    Map.entry('g', new char[]{'γ', 'г'}),
                    Map.entry('h', new char[]{'η', 'н'}),
                    Map.entry('i', new char[]{'ι', 'і'}),
                    Map.entry('j', new char[]{'ј'}),
                    Map.entry('k', new char[]{'κ', 'к'}),
                    Map.entry('l', new char[]{'λ', 'л'}),
                    Map.entry('m', new char[]{'μ', 'м'}),
                    Map.entry('n', new char[]{'ν'}),
                    Map.entry('o', new char[]{'ο', 'о'}),
                    Map.entry('p', new char[]{'ρ', 'р'}),
                    Map.entry('t', new char[]{'τ', 'т'}),
                    Map.entry('u', new char[]{'υ', 'у'}),
                    Map.entry('v', new char[]{'ѵ'}),
                    Map.entry('w', new char[]{'ω'}),
                    Map.entry('x', new char[]{'χ', 'х'}),
                    Map.entry('z', new char[]{'ζ', 'з'})
            );

    private static final Map<Character, Character> LEET =
            Map.of(
                    'a', '4',
                    'b', '8',
                    'e', '3',
                    'i', '1',
                    'o', '0',
                    's', '5',
                    't', '7'
            );

    public List<TestVariation> generate(String word) {
        if (word == null || word.isBlank()) {
            return List.of();
        }

        Set<TestVariation> variations =
                new LinkedHashSet<>();

        add(variations, "NORMAL", word);
        addRepeatVariations(variations, word);
        addSeparatorVariations(variations, word);
        addMixedSeparatorVariations(variations, word);
        addInvisibleVariations(variations, word);
        addCombiningVariations(variations, word);
        addConfusableVariations(variations, word);
        addMultiConfusableVariations(variations, word);
        addLeetVariations(variations, word);
        addMultiLeetVariations(variations, word);
        addCombinedVariations(variations, word);
        addExtremeVariations(variations, word);

        return List.copyOf(variations);
    }

    private void addRepeatVariations(
            Set<TestVariation> variations,
            String word
    ) {
        for (int index = 0; index < word.length(); index++) {
            for (int repeatCount : REPEAT_COUNTS) {
                StringBuilder value =
                        new StringBuilder(
                                word.length() + repeatCount
                        );

                for (int characterIndex = 0;
                     characterIndex < word.length();
                     characterIndex++) {

                    char character =
                            word.charAt(characterIndex);

                    value.append(character);

                    if (characterIndex == index) {
                        for (int count = 1;
                             count < repeatCount;
                             count++) {
                            value.append(character);
                        }
                    }
                }

                add(
                        variations,
                        "REPEAT",
                        value.toString()
                );
            }
        }
    }

    private void addSeparatorVariations(
            Set<TestVariation> variations,
            String word
    ) {
        for (char separator : SEPARATORS) {
            addBetweenCharacters(
                    variations,
                    "SEPARATOR",
                    word,
                    String.valueOf(separator)
            );
        }

        for (String separator : MULTI_SEPARATORS) {
            addBetweenCharacters(
                    variations,
                    "SEPARATOR",
                    word,
                    separator
            );
        }
    }

    private void addMixedSeparatorVariations(
            Set<TestVariation> variations,
            String word
    ) {
        if (word.length() < 2) {
            return;
        }

        for (char first : SEPARATORS) {
            for (char second : SEPARATORS) {
                StringBuilder result =
                        new StringBuilder(
                                word.length() * 2
                        );

                for (int index = 0;
                     index < word.length();
                     index++) {

                    if (index > 0) {
                        result.append(
                                index % 2 == 0
                                        ? first
                                        : second
                        );
                    }

                    result.append(
                            word.charAt(index)
                    );
                }

                add(
                        variations,
                        "MIXED_SEPARATOR",
                        result.toString()
                );
            }
        }
    }

    private void addInvisibleVariations(
            Set<TestVariation> variations,
            String word
    ) {
        for (char invisible : INVISIBLE_CHARACTERS) {
            for (int index = 1;
                 index < word.length();
                 index++) {

                String value =
                        word.substring(0, index)
                                + invisible
                                + word.substring(index);

                add(
                        variations,
                        "INVISIBLE",
                        value
                );
            }
        }
    }

    private void addCombiningVariations(
            Set<TestVariation> variations,
            String word
    ) {
        for (int index = 0;
             index < word.length();
             index++) {

            for (char mark : COMBINING_MARKS) {
                add(
                        variations,
                        "COMBINING",
                        insertAfter(
                                word,
                                index,
                                mark
                        )
                );
            }
        }
    }

    private void addConfusableVariations(
            Set<TestVariation> variations,
            String word
    ) {
        for (int index = 0;
             index < word.length();
             index++) {

            char character =
                    Character.toLowerCase(
                            word.charAt(index)
                    );

            char[] replacements =
                    CONFUSABLES.get(character);

            if (replacements == null) {
                continue;
            }

            for (char replacement : replacements) {
                add(
                        variations,
                        "UNICODE",
                        replaceAt(
                                word,
                                index,
                                replacement
                        )
                );
            }
        }
    }

    private void addMultiConfusableVariations(
            Set<TestVariation> variations,
            String word
    ) {
        for (int first = 0;
             first < word.length();
             first++) {

            char firstCharacter =
                    Character.toLowerCase(
                            word.charAt(first)
                    );

            char[] firstReplacements =
                    CONFUSABLES.get(firstCharacter);

            if (firstReplacements == null) {
                continue;
            }

            for (int second = first + 1;
                 second < word.length();
                 second++) {

                char secondCharacter =
                        Character.toLowerCase(
                                word.charAt(second)
                        );

                char[] secondReplacements =
                        CONFUSABLES.get(secondCharacter);

                if (secondReplacements == null) {
                    continue;
                }

                for (char firstReplacement : firstReplacements) {
                    for (char secondReplacement : secondReplacements) {
                        String value =
                                replaceAt(
                                        word,
                                        first,
                                        firstReplacement
                                );

                        value =
                                replaceAt(
                                        value,
                                        second,
                                        secondReplacement
                                );

                        add(
                                variations,
                                "UNICODE_COMBINED",
                                value
                        );
                    }
                }
            }
        }
    }

    private void addLeetVariations(
            Set<TestVariation> variations,
            String word
    ) {
        for (int index = 0;
             index < word.length();
             index++) {

            char character =
                    Character.toLowerCase(
                            word.charAt(index)
                    );

            Character replacement =
                    LEET.get(character);

            if (replacement == null) {
                continue;
            }

            add(
                    variations,
                    "LEET",
                    replaceAt(
                            word,
                            index,
                            replacement
                    )
            );
        }
    }

    private void addMultiLeetVariations(
            Set<TestVariation> variations,
            String word
    ) {
        StringBuilder result =
                new StringBuilder(word);

        boolean changed = false;

        for (int index = 0;
             index < word.length();
             index++) {

            char character =
                    Character.toLowerCase(
                            word.charAt(index)
                    );

            Character replacement =
                    LEET.get(character);

            if (replacement != null) {
                result.setCharAt(
                        index,
                        replacement
                );

                changed = true;
            }
        }

        if (changed) {
            add(
                    variations,
                    "LEET_COMBINED",
                    result.toString()
            );
        }
    }

    private void addCombinedVariations(
            Set<TestVariation> variations,
            String word
    ) {
        String repeated =
                repeatCharacters(
                        word,
                        2
                );

        for (String separator : MULTI_SEPARATORS) {
            addBetweenCharacters(
                    variations,
                    "REPEAT_SEPARATOR",
                    repeated,
                    separator
            );
        }

        String confusable =
                createConfusableVariant(word);

        if (!confusable.equals(word)) {
            for (String separator : MULTI_SEPARATORS) {
                addBetweenCharacters(
                        variations,
                        "UNICODE_SEPARATOR",
                        confusable,
                        separator
                );
            }
        }

        String leet =
                createLeetVariant(word);

        if (!leet.equals(word)) {
            for (String separator : MULTI_SEPARATORS) {
                addBetweenCharacters(
                        variations,
                        "LEET_SEPARATOR",
                        leet,
                        separator
                );
            }
        }

        String repeatedConfusable =
                repeatCharacters(
                        confusable,
                        2
                );

        if (!repeatedConfusable.equals(word)) {
            addBetweenCharacters(
                    variations,
                    "UNICODE_REPEAT_SEPARATOR",
                    repeatedConfusable,
                    "___"
            );

            addBetweenCharacters(
                    variations,
                    "UNICODE_REPEAT_SEPARATOR",
                    repeatedConfusable,
                    "---"
            );
        }
    }

    private void addExtremeVariations(
            Set<TestVariation> variations,
            String word
    ) {
        String confusable =
                createConfusableVariant(word);

        String leet =
                createLeetVariant(word);

        String repeated =
                repeatCharacters(
                        word,
                        4
                );

        String[] variants = {
                repeated,
                confusable,
                leet,
                repeatCharacters(confusable, 3),
                repeatCharacters(leet, 3)
        };

        String[] separators = {
                "---",
                "___",
                "...",
                "!_!",
                "-_-",
                "._.",
                "~_~"
        };

        for (String variant : variants) {
            if (variant.equals(word)) {
                continue;
            }

            for (String separator : separators) {
                addBetweenCharacters(
                        variations,
                        "EXTREME",
                        variant,
                        separator
                );
            }
        }
    }

    private String repeatCharacters(
            String word,
            int amount
    ) {
        StringBuilder result =
                new StringBuilder(
                        word.length() * amount
                );

        for (int index = 0;
             index < word.length();
             index++) {

            char character =
                    word.charAt(index);

            for (int count = 0;
                 count < amount;
                 count++) {
                result.append(character);
            }
        }

        return result.toString();
    }

    private String createConfusableVariant(
            String word
    ) {
        StringBuilder result =
                new StringBuilder(word);

        for (int index = 0;
             index < word.length();
             index++) {

            char character =
                    Character.toLowerCase(
                            word.charAt(index)
                    );

            char[] replacements =
                    CONFUSABLES.get(character);

            if (replacements != null
                    && replacements.length > 0) {

                result.setCharAt(
                        index,
                        replacements[0]
                );

                return result.toString();
            }
        }

        return word;
    }

    private String createLeetVariant(
            String word
    ) {
        StringBuilder result =
                new StringBuilder(word);

        for (int index = 0;
             index < word.length();
             index++) {

            char character =
                    Character.toLowerCase(
                            word.charAt(index)
                    );

            Character replacement =
                    LEET.get(character);

            if (replacement != null) {
                result.setCharAt(
                        index,
                        replacement
                );

                return result.toString();
            }
        }

        return word;
    }

    private void addBetweenCharacters(
            Set<TestVariation> variations,
            String category,
            String word,
            String separator
    ) {
        if (word.length() < 2) {
            return;
        }

        StringBuilder result =
                new StringBuilder(
                        word.length()
                                + separator.length()
                                * (word.length() - 1)
                );

        for (int index = 0;
             index < word.length();
             index++) {

            if (index > 0) {
                result.append(separator);
            }

            result.append(
                    word.charAt(index)
            );
        }

        add(
                variations,
                category,
                result.toString()
        );
    }

    private String insertAfter(
            String value,
            int index,
            char character
    ) {
        return value.substring(0, index + 1)
                + character
                + value.substring(index + 1);
    }

    private String replaceAt(
            String value,
            int index,
            char replacement
    ) {
        StringBuilder result =
                new StringBuilder(value);

        result.setCharAt(
                index,
                replacement
        );

        return result.toString();
    }

    private void add(
            Set<TestVariation> variations,
            String category,
            String value
    ) {
        if (value == null || value.isEmpty()) {
            return;
        }

        variations.add(
                new TestVariation(
                        category,
                        value
                )
        );
    }

    public record TestVariation(
            String category,
            String value
    ) {
    }
}