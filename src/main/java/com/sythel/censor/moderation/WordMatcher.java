package com.sythel.censor.moderation;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class WordMatcher {

    private static final Set<Integer> SEPARATOR_CODE_POINTS =
            Set.of(
                    (int) ' ',
                    (int) '\t',
                    (int) '\n',
                    (int) '\r',
                    (int) '.',
                    (int) ',',
                    (int) ';',
                    (int) ':',
                    (int) '!',
                    (int) '?',
                    (int) '_',
                    (int) '-',
                    (int) '=',
                    (int) '+',
                    (int) '/',
                    (int) '\\',
                    (int) '|',
                    (int) '~',
                    (int) '`',
                    (int) '\'',
                    (int) '"',
                    (int) '(',
                    (int) ')',
                    (int) '[',
                    (int) ']',
                    (int) '{',
                    (int) '}',
                    (int) '<',
                    (int) '>',
                    (int) '*',
                    (int) '^',
                    (int) '#',
                    (int) '%',
                    (int) '&'
            );

    private static final Set<Integer> INVISIBLE_CODE_POINTS =
            Set.of(
                    0x00AD,
                    0x034F,
                    0x061C,
                    0x115F,
                    0x1160,
                    0x17B4,
                    0x17B5,
                    0x180E,
                    0x200B,
                    0x200C,
                    0x200D,
                    0x200E,
                    0x200F,
                    0x202A,
                    0x202B,
                    0x202C,
                    0x202D,
                    0x202E,
                    0x2060,
                    0x2061,
                    0x2062,
                    0x2063,
                    0x2064,
                    0x2066,
                    0x2067,
                    0x2068,
                    0x2069,
                    0x206A,
                    0x206B,
                    0x206C,
                    0x206D,
                    0x206E,
                    0x206F,
                    0x2800,
                    0x3000,
                    0x3164,
                    0xFEFF
            );

    private volatile MatcherData matcherData =
            MatcherData.empty();

    public void reload(List<String> blockedWords) {
        if (blockedWords == null || blockedWords.isEmpty()) {
            matcherData = MatcherData.empty();
            return;
        }

        Map<String, BlockedWord> uniqueWords =
                new HashMap<>();

        for (String blockedWord : blockedWords) {
            if (!isUsableWord(blockedWord)) {
                continue;
            }

            String normalized =
                    normalizeWord(blockedWord.trim());

            if (normalized.isEmpty()) {
                continue;
            }

            BlockedWord word =
                    createBlockedWord(normalized);

            if (word.characters().isEmpty()) {
                continue;
            }

            uniqueWords.putIfAbsent(
                    word.compactWord(),
                    word
            );
        }

        if (uniqueWords.isEmpty()) {
            matcherData = MatcherData.empty();
            return;
        }

        List<BlockedWord> words =
                uniqueWords.values()
                        .stream()
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                BlockedWord::visibleLength
                                        )
                                        .reversed()
                                        .thenComparing(
                                                BlockedWord::word
                                        )
                        )
                        .toList();

        TrieNode root =
                new TrieNode();

        for (int index = 0;
             index < words.size();
             index++) {

            BlockedWord word =
                    words.get(index);

            TrieNode node =
                    root;

            for (char character :
                    word.characters()) {

                node =
                        node.children.computeIfAbsent(
                                character,
                                ignored -> new TrieNode()
                        );
            }

            node.outputs.add(index);
        }

        buildFailureLinks(root);

        matcherData =
                new MatcherData(
                        root,
                        List.copyOf(words)
                );
    }

    public CensorResult censor(
            String message,
            String replacement
    ) {
        if (message == null || message.isEmpty()) {
            return emptyResult(message);
        }

        MatcherData data =
                matcherData;

        if (data.words().isEmpty()) {
            return emptyResult(message);
        }

        NormalizedMessage normalizedMessage =
                normalizeMessage(message);

        if (normalizedMessage.characters().isEmpty()) {
            return emptyResult(message);
        }

        List<Match> matches =
                findMatches(
                        normalizedMessage,
                        data
                );

        if (matches.isEmpty()) {
            return emptyResult(message);
        }

        List<Match> mergedMatches =
                mergeMatches(matches);

        String censoredMessage =
                applyReplacements(
                        message,
                        replacement,
                        mergedMatches
                );

        Set<String> matchedWords =
                new LinkedHashSet<>();

        for (Match match : matches) {
            matchedWords.add(match.word());
        }

        return new CensorResult(
                message,
                censoredMessage,
                true,
                List.copyOf(matchedWords),
                List.of(ModerationType.WORD)
        );
    }

    private List<Match> findMatches(
            NormalizedMessage message,
            MatcherData data
    ) {
        List<Match> matches =
                new ArrayList<>();

        TrieNode root =
                data.root();

        TrieNode state =
                root;

        List<NormalizedCharacter> characters =
                message.characters();

        for (int position = 0;
             position < characters.size();
             position++) {

            NormalizedCharacter current =
                    characters.get(position);

            char character =
                    current.canonicalCharacter();

            TrieNode next =
                    state.children.get(character);

            while (state != root
                    && next == null) {

                state =
                        state.failure;

                next =
                        state.children.get(character);
            }

            state =
                    next != null
                            ? next
                            : root;

            if (state.outputs.isEmpty()) {
                continue;
            }

            for (int wordIndex :
                    state.outputs) {

                BlockedWord word =
                        data.words().get(wordIndex);

                int patternLength =
                        word.characters().size();

                int start =
                        position - patternLength + 1;

                if (start < 0) {
                    continue;
                }

                if (!isBoundaryValid(
                        message,
                        start,
                        position
                )) {
                    continue;
                }

                NormalizedCharacter first =
                        characters.get(start);

                NormalizedCharacter last =
                        characters.get(position);

                matches.add(
                        new Match(
                                word.word(),
                                first.originalStart(),
                                last.originalEnd(),
                                word.visibleLength()
                        )
                );
            }
        }

        return matches;
    }

    private boolean isBoundaryValid(
            NormalizedMessage message,
            int start,
            int end
    ) {
        NormalizedCharacter first =
                message.characters().get(start);

        NormalizedCharacter last =
                message.characters().get(end);

        int previous =
                message.previousVisibleCharacter(
                        first.originalStart()
                );

        if (previous >= 0
                && isWordCharacterAt(
                message.original(),
                previous
        )) {
            return false;
        }

        int next =
                message.nextVisibleCharacter(
                        last.originalEnd()
                );

        if (next >= 0
                && isWordCharacterAt(
                message.original(),
                next
        )) {
            return false;
        }

        return true;
    }

    private List<Match> mergeMatches(
            List<Match> matches
    ) {
        if (matches.size() <= 1) {
            return matches;
        }

        List<Match> sorted =
                matches.stream()
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                Match::start
                                        )
                                        .thenComparing(
                                                Comparator
                                                        .comparingInt(
                                                                Match::end
                                                        )
                                                        .reversed()
                                        )
                                        .thenComparing(
                                                Comparator
                                                        .comparingInt(
                                                                Match::wordLength
                                                        )
                                                        .reversed()
                                        )
                        )
                        .toList();

        List<Match> merged =
                new ArrayList<>();

        Match current =
                sorted.get(0);

        for (int i = 1;
             i < sorted.size();
             i++) {

            Match next =
                    sorted.get(i);

            if (next.start() < current.end()) {
                if (next.end() > current.end()) {
                    current =
                            new Match(
                                    current.word(),
                                    current.start(),
                                    next.end(),
                                    Math.max(
                                            current.wordLength(),
                                            next.wordLength()
                                    )
                            );
                }

                continue;
            }

            merged.add(current);
            current = next;
        }

        merged.add(current);

        return List.copyOf(merged);
    }

    private String applyReplacements(
            String message,
            String replacement,
            List<Match> matches
    ) {
        String safeReplacement =
                replacement == null
                        ? "***"
                        : replacement;

        StringBuilder result =
                new StringBuilder(
                        message.length()
                );

        int cursor = 0;

        for (Match match :
                matches) {

            if (match.start() < cursor) {
                continue;
            }

            result.append(
                    message,
                    cursor,
                    match.start()
            );

            result.append(
                    safeReplacement
            );

            cursor =
                    match.end();
        }

        result.append(
                message,
                cursor,
                message.length()
        );

        return result.toString();
    }

    private void buildFailureLinks(
            TrieNode root
    ) {
        Queue<TrieNode> queue =
                new ArrayDeque<>();

        root.failure =
                root;

        for (TrieNode child :
                root.children.values()) {

            child.failure =
                    root;

            queue.add(child);
        }

        while (!queue.isEmpty()) {
            TrieNode current =
                    queue.remove();

            for (Map.Entry<Character, TrieNode> entry :
                    current.children.entrySet()) {

                char character =
                        entry.getKey();

                TrieNode child =
                        entry.getValue();

                TrieNode failure =
                        current.failure;

                TrieNode fallback =
                        failure.children.get(character);

                while (failure != root
                        && fallback == null) {

                    failure =
                            failure.failure;

                    fallback =
                            failure.children.get(character);
                }

                if (fallback != null
                        && fallback != child) {

                    child.failure =
                            fallback;
                } else {
                    child.failure =
                            root;
                }

                if (!child.failure.outputs.isEmpty()) {
                    child.outputs.addAll(
                            child.failure.outputs
                    );
                }

                queue.add(child);
            }
        }
    }

    private BlockedWord createBlockedWord(
            String normalized
    ) {
        List<Character> characters =
                new ArrayList<>();

        StringBuilder compact =
                new StringBuilder();

        for (int offset = 0;
             offset < normalized.length();) {

            int codePoint =
                    normalized.codePointAt(offset);

            offset +=
                    Character.charCount(codePoint);

            if (isInvisible(codePoint)
                    || isSeparator(codePoint)
                    || isCombiningMark(codePoint)) {
                continue;
            }

            char canonical =
                    canonicalCharacter(codePoint);

            if (!Character.isLetterOrDigit(
                    canonical
            )) {
                continue;
            }

            if (!characters.isEmpty()
                    && characters.get(
                    characters.size() - 1
            ) == canonical) {
                continue;
            }

            characters.add(canonical);
            compact.append(canonical);
        }

        return new BlockedWord(
                normalized,
                compact.toString(),
                List.copyOf(characters),
                countCanonicalCharacters(normalized)
        );
    }

    private NormalizedMessage normalizeMessage(
            String message
    ) {
        List<NormalizedCharacter> characters =
                new ArrayList<>();

        for (int offset = 0;
             offset < message.length();) {

            int originalStart =
                    offset;

            int codePoint =
                    message.codePointAt(offset);

            offset +=
                    Character.charCount(codePoint);

            if (isInvisible(codePoint)) {
                continue;
            }

            if (codePoint < 128) {
                if (isSeparator(codePoint)) {
                    continue;
                }

                if (isCombiningMark(codePoint)) {
                    continue;
                }

                char canonical =
                        canonicalCharacter(codePoint);

                if (!Character.isLetterOrDigit(
                        canonical
                )) {
                    continue;
                }

                NormalizedCharacter previous =
                        characters.isEmpty()
                                ? null
                                : characters.get(
                                characters.size() - 1
                        );

                if (previous != null
                        && previous.canonicalCharacter()
                        == canonical) {

                    previous.updateEnd(offset);
                    continue;
                }

                characters.add(
                        new NormalizedCharacter(
                                canonical,
                                originalStart,
                                offset
                        )
                );

                continue;
            }

            String value =
                    new String(
                            Character.toChars(codePoint)
                    );

            value =
                    Normalizer.normalize(
                            value,
                            Normalizer.Form.NFKC
                    )
                    .toLowerCase(Locale.ROOT);

            for (int index = 0;
                 index < value.length();) {

                int normalizedCodePoint =
                        value.codePointAt(index);

                index +=
                        Character.charCount(
                                normalizedCodePoint
                        );

                if (isInvisible(normalizedCodePoint)
                        || isCombiningMark(
                        normalizedCodePoint
                )
                        || isSeparator(
                        normalizedCodePoint
                )) {
                    continue;
                }

                char canonical =
                        canonicalCharacter(
                                normalizedCodePoint
                        );

                if (!Character.isLetterOrDigit(
                        canonical
                )) {
                    continue;
                }

                NormalizedCharacter previous =
                        characters.isEmpty()
                                ? null
                                : characters.get(
                                characters.size() - 1
                        );

                if (previous != null
                        && previous.canonicalCharacter()
                        == canonical) {

                    previous.updateEnd(offset);
                    continue;
                }

                characters.add(
                        new NormalizedCharacter(
                                canonical,
                                originalStart,
                                offset
                        )
                );
            }
        }

        return new NormalizedMessage(
                message,
                List.copyOf(characters)
        );
    }

    private String normalizeWord(
            String word
    ) {
        return Normalizer.normalize(
                        word,
                        Normalizer.Form.NFKC
                )
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private char canonicalCharacter(
            int codePoint
    ) {
        if (codePoint > Character.MAX_VALUE) {
            return 0;
        }

        char character =
                Character.toLowerCase(
                        (char) codePoint
                );

        character =
                normalizeLatinCharacter(
                        character
                );

        character =
                normalizeConfusableCharacter(
                        character
                );

        character =
                normalizeLeetCharacter(
                        character
                );

        return character;
    }

    private char normalizeLatinCharacter(
            char character
    ) {
        return switch (character) {
            case 'à', 'á', 'â', 'ã', 'ä', 'å',
                 'ā', 'ă', 'ą', 'ǎ', 'ǟ', 'ǡ',
                 'ǻ', 'ȁ', 'ȃ', 'ȧ', 'ɑ' ->
                    'a';

            case 'ç', 'ć', 'ĉ', 'ċ', 'č',
                 'ƈ', 'ȼ', 'ḉ' ->
                    'c';

            case 'ď', 'đ', 'ƌ', 'ḓ',
                 'ḋ', 'ḍ', 'ḏ', 'ɗ' ->
                    'd';

            case 'è', 'é', 'ê', 'ë',
                 'ē', 'ĕ', 'ė', 'ę', 'ě',
                 'ȅ', 'ȇ', 'ẹ', 'ẻ', 'ẽ',
                 'ế', 'ề', 'ể', 'ễ', 'ệ' ->
                    'e';

            case 'ğ', 'ĝ', 'ġ', 'ģ',
                 'ǵ', 'ɠ', 'ḡ' ->
                    'g';

            case 'ĥ', 'ħ', 'ȟ', 'ḧ',
                 'ḩ', 'ḥ', 'ɦ' ->
                    'h';

            case 'ì', 'í', 'î', 'ï',
                 'ĩ', 'ī', 'ĭ', 'į',
                 'ı', 'ǐ', 'ȉ', 'ȋ',
                 'ɨ' ->
                    'i';

            case 'ĵ', 'ǰ', 'ɉ' ->
                    'j';

            case 'ķ', 'ǩ', 'ḱ', 'ḳ',
                 'ḵ', 'ƙ' ->
                    'k';

            case 'ĺ', 'ļ', 'ľ', 'ŀ',
                 'ł', 'ƚ', 'ḷ', 'ḹ',
                 'ḽ' ->
                    'l';

            case 'ḿ', 'ṁ', 'ṃ', 'ɱ' ->
                    'm';

            case 'ñ', 'ń', 'ņ', 'ň',
                 'ŉ', 'ŋ', 'ṅ', 'ṇ',
                 'ṉ', 'ɲ' ->
                    'n';

            case 'ò', 'ó', 'ô', 'õ', 'ö',
                 'ø', 'ō', 'ŏ', 'ő', 'ơ',
                 'ǒ', 'ǫ', 'ǭ', 'ȍ', 'ȏ',
                 'ọ', 'ỏ', 'ố', 'ồ', 'ổ',
                 'ỗ', 'ộ', 'ớ', 'ờ', 'ở',
                 'ỡ', 'ợ' ->
                    'o';

            case 'ƥ', 'ṕ', 'ṗ' ->
                    'p';

            case 'ŕ', 'ŗ', 'ř', 'ȑ',
                 'ȓ', 'ɍ', 'ṙ', 'ṛ',
                 'ṝ' ->
                    'r';

            case 'ś', 'ŝ', 'ş', 'š',
                 'ſ', 'ș', 'ṡ', 'ṣ',
                 'ṧ', 'ṩ' ->
                    's';

            case 'ţ', 'ť', 'ŧ', 'ț',
                 'ṫ', 'ṭ', 'ṯ' ->
                    't';

            case 'ù', 'ú', 'û', 'ü',
                 'ũ', 'ū', 'ŭ', 'ů',
                 'ű', 'ų', 'ư', 'ǔ',
                 'ǖ', 'ǘ', 'ǚ', 'ǜ',
                 'ụ', 'ủ', 'ứ', 'ừ',
                 'ử', 'ữ', 'ự' ->
                    'u';

            case 'ṽ', 'ṿ', 'ʋ' ->
                    'v';

            case 'ŵ', 'ẁ', 'ẃ', 'ẅ',
                 'ẇ' ->
                    'w';

            case 'ẋ', 'ẍ' ->
                    'x';

            case 'ý', 'ÿ', 'ŷ', 'ȳ',
                 'ẏ', 'ỵ', 'ỷ', 'ỹ' ->
                    'y';

            case 'ź', 'ż', 'ž', 'ƶ',
                 'ẑ', 'ẓ', 'ẕ' ->
                    'z';

            default ->
                    character;
        };
    }

    private char normalizeConfusableCharacter(
            char character
    ) {
        return switch (character) {
            case 'α', 'а' ->
                    'a';

            case 'β', 'в' ->
                    'b';

            case 'ϲ', 'с' ->
                    'c';

            case 'δ', 'д' ->
                    'd';

            case 'ε', 'е' ->
                    'e';

            case 'ф' ->
                    'f';

            case 'γ', 'г' ->
                    'g';

            case 'η', 'н' ->
                    'h';

            case 'ι', 'і' ->
                    'i';

            case 'ј' ->
                    'j';

            case 'κ', 'к' ->
                    'k';

            case 'λ', 'л' ->
                    'l';

            case 'μ', 'м' ->
                    'm';

            case 'ο', 'о' ->
                    'o';

            case 'ρ', 'р' ->
                    'p';

            case 'τ', 'т' ->
                    't';

            case 'υ', 'у' ->
                    'u';

            case 'ѵ' ->
                    'v';

            case 'ω' ->
                    'w';

            case 'χ', 'х' ->
                    'x';

            case 'ζ', 'з' ->
                    'z';

            case 'ν' ->
                    'n';

            default ->
                    character;
        };
    }

    private char normalizeLeetCharacter(
            char character
    ) {
        return switch (character) {
            case '4' ->
                    'a';

            case '8' ->
                    'b';

            case '3' ->
                    'e';

            case '1' ->
                    'i';

            case '0' ->
                    'o';

            case '5', '$' ->
                    's';

            case '7' ->
                    't';

            default ->
                    character;
        };
    }

    private boolean isInvisible(
            int codePoint
    ) {
        return INVISIBLE_CODE_POINTS.contains(
                codePoint
        );
    }

    private boolean isSeparator(
            int codePoint
    ) {
        return SEPARATOR_CODE_POINTS.contains(
                codePoint
        );
    }

    private boolean isCombiningMark(
            int codePoint
    ) {
        int type =
                Character.getType(
                        codePoint
                );

        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    private boolean isWordCharacterAt(
            String value,
            int index
    ) {
        if (index < 0
                || index >= value.length()) {
            return false;
        }

        int codePoint =
                value.codePointAt(index);

        return Character.isLetterOrDigit(
                codePoint
        ) || codePoint == '_';
    }

    private boolean isUsableWord(
            String word
    ) {
        if (word == null || word.isBlank()) {
            return false;
        }

        for (int offset = 0;
             offset < word.length();) {

            int codePoint =
                    word.codePointAt(offset);

            offset +=
                    Character.charCount(codePoint);

            if (Character.isLetterOrDigit(
                    codePoint
            )) {
                return true;
            }
        }

        return false;
    }

    private int countCanonicalCharacters(
            String value
    ) {
        int length = 0;

        for (int offset = 0;
             offset < value.length();) {

            int codePoint =
                    value.codePointAt(offset);

            offset +=
                    Character.charCount(codePoint);

            if (isInvisible(codePoint)
                    || isSeparator(codePoint)
                    || isCombiningMark(codePoint)) {
                continue;
            }

            char canonical =
                    canonicalCharacter(
                            codePoint
                    );

            if (Character.isLetterOrDigit(
                    canonical
            )) {
                length++;
            }
        }

        return length;
    }

    private CensorResult emptyResult(
            String message
    ) {
        return new CensorResult(
                message,
                message,
                false,
                List.of(),
                List.of()
        );
    }

    private static final class TrieNode {

        private final Map<Character, TrieNode> children =
                new HashMap<>();

        private final List<Integer> outputs =
                new ArrayList<>();

        private TrieNode failure;
    }

    private static final class NormalizedCharacter {

        private final char canonicalCharacter;
        private final int originalStart;
        private int originalEnd;

        private NormalizedCharacter(
                char canonicalCharacter,
                int originalStart,
                int originalEnd
        ) {
            this.canonicalCharacter =
                    canonicalCharacter;

            this.originalStart =
                    originalStart;

            this.originalEnd =
                    originalEnd;
        }

        private char canonicalCharacter() {
            return canonicalCharacter;
        }

        private int originalStart() {
            return originalStart;
        }

        private int originalEnd() {
            return originalEnd;
        }

        private void updateEnd(
                int newEnd
        ) {
            originalEnd =
                    newEnd;
        }
    }

    private record NormalizedMessage(
            String original,
            List<NormalizedCharacter> characters
    ) {

        private int previousVisibleCharacter(
                int index
        ) {
            int offset =
                    index - 1;

            while (offset >= 0) {
                int codePoint =
                        original.codePointBefore(
                                offset + 1
                        );

                int start =
                        offset
                                - Character.charCount(
                                codePoint
                        )
                                + 1;

                if (!INVISIBLE_CODE_POINTS.contains(
                        codePoint
                )) {
                    return start;
                }

                offset =
                        start - 1;
            }

            return -1;
        }

        private int nextVisibleCharacter(
                int index
        ) {
            int offset =
                    index;

            while (offset < original.length()) {
                int codePoint =
                        original.codePointAt(
                                offset
                        );

                if (!INVISIBLE_CODE_POINTS.contains(
                        codePoint
                )) {
                    return offset;
                }

                offset +=
                        Character.charCount(
                                codePoint
                        );
            }

            return -1;
        }
    }

    private record BlockedWord(
            String word,
            String compactWord,
            List<Character> characters,
            int visibleLength
    ) {
    }

    private record Match(
            String word,
            int start,
            int end,
            int wordLength
    ) {
    }

    private record MatcherData(
            TrieNode root,
            List<BlockedWord> words
    ) {

        private static MatcherData empty() {
            return new MatcherData(
                    new TrieNode(),
                    List.of()
            );
        }
    }
}
