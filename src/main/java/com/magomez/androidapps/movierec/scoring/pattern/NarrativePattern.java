package com.magomez.androidapps.movierec.scoring.pattern;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The catalogue of narrative taste patterns the system knows how to look for.
 *
 * <p>Each constant is a <b>deterministic signature</b> over a film's genres (and, for one
 * pattern, its country) — a proxy built only from data TMDB actually gives us. The genre
 * names are TMDB's Spanish labels ({@code es-ES}), compared accent- and case-insensitively.
 *
 * <p>A pattern in this enum is just a definition. Whether the <em>user</em> has it (and
 * how strongly) is decided by {@code TastePatternDetector} against their Letterboxd
 * history; whether a <em>candidate</em> film fits it is {@link #matches(Set, Set)}.
 *
 * <p>These are intentionally simple first proxies. TMDB has no "absurd" or "uncomfortable"
 * genre, so those are approximated (comedy + high-concept genres; drama + dark genres);
 * the approximation is documented in each {@link #description()} so nothing is a black box.
 */
public enum NarrativePattern {

    KOREAN_THRILLER(
            "Thriller coreano",
            "Cine de Corea del Sur con thriller, misterio o crimen: tensión y personajes moralmente ambiguos.",
            "combina el thriller coreano con esa tensión y esos personajes moralmente ambiguos") {
        @Override
        public boolean matches(Set<String> genres, Set<String> countries) {
            return containsAny(countries, "corea del sur", "south korea")
                    && containsAny(genres, "suspense", "misterio", "crimen", "thriller");
        }
    },

    BLACK_COMEDY(
            "Comedia negra",
            "Comedia cruzada con terror u horror (a menudo gore): humor con un fondo macabro. "
                    + "No cuenta comedia + crimen/drama/thriller, que suelen ser dramedias o "
                    + "comedias de acción, no comedia negra.",
            "mezcla la comedia con lo macabro y lo gore, al estilo de la comedia negra") {
        @Override
        public boolean matches(Set<String> genres, Set<String> countries) {
            return containsAny(genres, "comedia")
                    && containsAny(genres, "terror", "horror");
        }
    },

    ABSURD_COMEDY(
            "Comedia absurda",
            "Comedia + ciencia ficción o fantasía, excluyendo animación, familia, aventura y acción. "
                    + "TMDB no marca el humor absurdo, así que este proxy es deliberadamente estrecho: "
                    + "prefiere poca evidencia a falsos positivos (Pixar, superhéroes).",
            "tira de humor absurdo y de premisas desatadas") {
        @Override
        public boolean matches(Set<String> genres, Set<String> countries) {
            return containsAny(genres, "comedia")
                    && containsAny(genres, "ciencia ficcion", "fantasia")
                    && !containsAny(genres, "animacion", "familia", "aventura", "accion");
        }
    },

    PSYCHOLOGICAL_HORROR(
            "Terror psicológico",
            "Terror cruzado con suspense, misterio o drama: perturbador más que de sustos.",
            "apuesta por el terror psicológico y perturbador más que por el susto fácil") {
        @Override
        public boolean matches(Set<String> genres, Set<String> countries) {
            return containsAny(genres, "terror", "horror")
                    && containsAny(genres, "suspense", "misterio", "drama", "thriller");
        }
    },

    UNCOMFORTABLE_CINEMA(
            "Cine incómodo",
            "Drama cruzado con terror, horror o suspense, sin comedia y sin acción: cine perturbador "
                    + "que no busca entretener sin más.",
            "es de ese cine incómodo que no busca ponértelo fácil") {
        @Override
        public boolean matches(Set<String> genres, Set<String> countries) {
            return containsAny(genres, "drama")
                    && containsAny(genres, "terror", "horror", "suspense")
                    && !containsAny(genres, "comedia")
                    && !containsAny(genres, "accion");
        }
    },

    THOUGHT_PROVOKING(
            "Cine que deja poso",
            "Drama cruzado con ciencia ficción, historia o bélica: cine de época, de guerra o especulativo "
                    + "que da que pensar. No cuenta drama + misterio (eso es thriller) ni documental suelto.",
            "es de las películas que dejan poso y dan que pensar") {
        @Override
        public boolean matches(Set<String> genres, Set<String> countries) {
            return containsAny(genres, "drama", "documental")
                    && containsAny(genres, "ciencia ficcion", "historia", "belica");
        }
    };

    private final String displayName;
    private final String description;
    private final String reasonFragment;

    NarrativePattern(String displayName, String description, String reasonFragment) {
        this.displayName = displayName;
        this.description = description;
        this.reasonFragment = reasonFragment;
    }

    /** Stable id used in {@code TastePattern.id} and for cross-referencing. */
    public String id() {
        return name();
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    /**
     * The cinéfile sentence fragment for a recommendation reason, e.g.
     * {@code "combina el thriller coreano con..."}. The caller adds the "how present in
     * your history" tail based on {@link TastePattern#strength()}.
     */
    public String reasonFragment() {
        return reasonFragment;
    }

    /** Hard gate: does a film with these (normalized) genre / country names fit the signature? */
    public abstract boolean matches(Set<String> genres, Set<String> countries);

    /** Convenience: normalizes the raw name collections and applies the hard gate. */
    public boolean matchesRaw(Iterable<String> genres, Iterable<String> countries) {
        return matches(normalizeAll(genres), normalizeAll(countries));
    }

    /**
     * <b>patternMatch</b>: how strongly a concrete film fits this pattern, in {@code [0, 1]}.
     *
     * <p>{@code 0} when the film does not meet the signature; otherwise a graded value —
     * a base (0.5 for {@code KOREAN_THRILLER}, 0.4 elsewhere) plus a span that grows with
     * how many of the pattern's trigger genres the film carries. This is a property of the
     * <em>film</em>, entirely separate from the pattern's {@code profileStrength} (how much
     * the pattern characterises the user).
     */
    public double match(Iterable<String> genres, Iterable<String> countries) {
        if (!matchesRaw(genres, countries)) {
            return 0.0;
        }
        int triggers = triggerGenresIn(genres).size();
        double base = this == KOREAN_THRILLER ? 0.5 : 0.4;
        double span = this == KOREAN_THRILLER ? 0.5 : 0.6;
        double cap = this == KOREAN_THRILLER ? 2.0 : 3.0;
        double value = base + span * Math.min(1.0, triggers / cap);
        return Math.round(value * 100.0) / 100.0;
    }

    /** The trigger genres of this pattern that appear in the given (raw) genre names. */
    public List<String> triggerGenresIn(Iterable<String> rawGenres) {
        Set<String> normalized = normalizeAll(rawGenres);
        return switch (this) {
            case KOREAN_THRILLER -> keep(rawGenres, normalized, "suspense", "misterio", "crimen", "thriller");
            case BLACK_COMEDY -> keep(rawGenres, normalized, "comedia", "terror", "horror");
            case ABSURD_COMEDY -> keep(rawGenres, normalized, "comedia", "ciencia ficcion", "fantasia");
            case PSYCHOLOGICAL_HORROR -> keep(rawGenres, normalized, "terror", "horror", "suspense", "misterio", "drama", "thriller");
            case UNCOMFORTABLE_CINEMA -> keep(rawGenres, normalized, "drama", "terror", "horror", "suspense");
            case THOUGHT_PROVOKING -> keep(rawGenres, normalized, "drama", "ciencia ficcion", "historia", "belica", "documental");
        };
    }

    // --- helpers -------------------------------------------------------------

    static Set<String> normalizeAll(Iterable<String> values) {
        if (values == null) {
            return Set.of();
        }
        Set<String> out = new java.util.LinkedHashSet<>();
        for (String v : values) {
            String n = normalize(v);
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
        return out;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ");
    }

    private static boolean containsAny(Set<String> normalized, String... targets) {
        for (String t : targets) {
            if (normalized.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> keep(Iterable<String> rawGenres, Set<String> normalized, String... triggers) {
        Set<String> triggerSet = Set.of(triggers);
        List<String> kept = new java.util.ArrayList<>();
        if (rawGenres != null) {
            for (String raw : rawGenres) {
                if (triggerSet.contains(normalize(raw)) && !kept.contains(raw)) {
                    kept.add(raw);
                }
            }
        }
        return kept.stream().collect(Collectors.toUnmodifiableList());
    }
}
