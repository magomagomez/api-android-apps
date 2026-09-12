package com.magomez.androidapps.movierec.provider.festival;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The films that played one edition of a festival, ready to be matched by title.
 *
 * @param festival festival name, e.g. {@code "Sundance"} (never blank)
 * @param year     the edition year
 * @param entries  the films, in source order
 */
public record FestivalLineup(String festival, int year, List<LineupEntry> entries) {

    public FestivalLineup {
        Objects.requireNonNull(festival, "festival");
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    /** The first entry whose title matches {@code title} (accent- and punctuation-insensitive). */
    public Optional<LineupEntry> lookup(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        String target = normalizeTitle(title);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        return entries.stream()
                .filter(e -> e.title() != null && normalizeTitle(e.title()).equals(target))
                .findFirst();
    }

    public static String normalizeTitle(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }
}
