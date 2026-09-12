package com.magomez.androidapps.movierec.scoring;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/**
 * Stable identity for a person (director / actor) across name spellings.
 *
 * <p>The identity key is the <b>TMDB person id</b> when known ({@code "id:<n>"}); it falls
 * back to a normalized name ({@code "nm:<normalized>"}) when the id is missing. So the same
 * director credited as {@code "Bong Joon-ho"} and {@code "봉준호"} — same TMDB person —
 * groups under one key, while two different name-only people stay separate.
 *
 * <p>Pure, deterministic, no external dependencies. Not a domain value object (records
 * carry their own {@code tmdbId} already); just a keying helper.
 */
public final class PersonKeys {

    private PersonKeys() {
    }

    public static String identityKey(Integer personId, String name) {
        return personId != null ? "id:" + personId : "nm:" + normalizeName(name);
    }

    public static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    /** True when the string contains at least one ASCII letter (i.e. a Latin-script name). */
    public static boolean hasLatinLetters(String value) {
        return value != null && value.chars().anyMatch(c -> c < 128 && Character.isLetter(c));
    }

    /**
     * Chooses the display name for a person from every spelling seen (name &rarr; count):
     * prefer a Latin-script spelling, then the most frequent, then alphabetical order.
     */
    public static String chooseDisplayName(Map<String, Integer> nameCounts) {
        return nameCounts.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<String, Integer> e) -> hasLatinLetters(e.getKey())).reversed()
                        .thenComparing(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue()).reversed())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }
}
