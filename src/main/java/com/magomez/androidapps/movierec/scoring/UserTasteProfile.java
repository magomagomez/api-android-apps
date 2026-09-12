package com.magomez.androidapps.movierec.scoring;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Director;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable, weighted snapshot of a user's cinematographic tastes.
 *
 * <p>Each dimension is a map {@code value -> affinity in [0, 1]}, where {@code 1.0} is
 * the user's strongest preference in that dimension. The weights are produced
 * deterministically by {@link UserTasteProfileBuilder}.
 *
 * <p><b>Person identity.</b> {@code directorAffinity} / {@code actorAffinity} are keyed
 * by a readable display name, but the builder groups people by their <b>TMDB person id</b>
 * first, so a director credited once as {@code "Bong Joon-ho"} and once as
 * {@code "봉준호"} counts once. {@link #directorIdToName()} / {@link #actorIdToName()} map
 * that person id to the display-name key; use {@link #directorAffinityOf(Director)} /
 * {@link #actorAffinityOf(Actor)} to resolve a movie's person against the profile (by id
 * when available, else by normalized name).
 *
 * <p>Pure data, no external dependencies, never persisted.
 *
 * @param directorAffinity display name &rarr; affinity {@code [0, 1]}
 * @param genreAffinity    genre name &rarr; affinity {@code [0, 1]}
 * @param actorAffinity    display name &rarr; affinity {@code [0, 1]}
 * @param countryAffinity  country name &rarr; affinity {@code [0, 1]}
 * @param decadeAffinity   decade (first year) &rarr; affinity {@code [0, 1]}
 * @param patterns         narrative taste patterns derived from the history, strongest first
 * @param directorIdToName TMDB person id &rarr; the display-name key used in {@code directorAffinity}
 * @param actorIdToName    TMDB person id &rarr; the display-name key used in {@code actorAffinity}
 */
public record UserTasteProfile(
        Map<String, Double> directorAffinity,
        Map<String, Double> genreAffinity,
        Map<String, Double> actorAffinity,
        Map<String, Double> countryAffinity,
        Map<Integer, Double> decadeAffinity,
        List<TastePattern> patterns,
        Map<Integer, String> directorIdToName,
        Map<Integer, String> actorIdToName) {

    public UserTasteProfile {
        directorAffinity = affinityMap(directorAffinity);
        genreAffinity = affinityMap(genreAffinity);
        actorAffinity = affinityMap(actorAffinity);
        countryAffinity = affinityMap(countryAffinity);
        decadeAffinity = decadeAffinityMap(decadeAffinity);
        patterns = patterns == null ? List.of() : List.copyOf(patterns);
        directorIdToName = directorIdToName == null ? Map.of() : Map.copyOf(directorIdToName);
        actorIdToName = actorIdToName == null ? Map.of() : Map.copyOf(actorIdToName);
    }

    /** Weighted profile with patterns but no person-id index. */
    public UserTasteProfile(Map<String, Double> directorAffinity, Map<String, Double> genreAffinity,
                            Map<String, Double> actorAffinity, Map<String, Double> countryAffinity,
                            Map<Integer, Double> decadeAffinity, List<TastePattern> patterns) {
        this(directorAffinity, genreAffinity, actorAffinity, countryAffinity, decadeAffinity,
                patterns, Map.of(), Map.of());
    }

    /** Weighted profile without narrative patterns (defaults to none). */
    public UserTasteProfile(Map<String, Double> directorAffinity, Map<String, Double> genreAffinity,
                            Map<String, Double> actorAffinity, Map<String, Double> countryAffinity,
                            Map<Integer, Double> decadeAffinity) {
        this(directorAffinity, genreAffinity, actorAffinity, countryAffinity, decadeAffinity, List.of());
    }

    /** Backward-compatible view: a plain preference set, every entry weighted equally ({@code 1.0}). */
    public UserTasteProfile(Set<String> directors, Set<String> genres, Set<String> actors,
                            Set<String> countries, Set<Integer> decades) {
        this(uniform(directors), uniform(genres), uniform(actors), uniform(countries),
                uniformInts(decades), List.of());
    }

    public static UserTasteProfile empty() {
        return new UserTasteProfile(Set.<String>of(), Set.<String>of(), Set.<String>of(),
                Set.<String>of(), Set.<Integer>of());
    }

    /** Returns a copy of this profile carrying the given narrative patterns. */
    public UserTasteProfile withPatterns(List<TastePattern> patterns) {
        return new UserTasteProfile(directorAffinity, genreAffinity, actorAffinity,
                countryAffinity, decadeAffinity, patterns, directorIdToName, actorIdToName);
    }

    public Set<String> preferredDirectors() {
        return directorAffinity.keySet();
    }

    public Set<String> preferredGenres() {
        return genreAffinity.keySet();
    }

    public Set<String> preferredActors() {
        return actorAffinity.keySet();
    }

    public Set<String> preferredCountries() {
        return countryAffinity.keySet();
    }

    public Set<Integer> preferredDecades() {
        return decadeAffinity.keySet();
    }

    /** @return the weighted affinity in {@code [0, 1]}, matching by normalized name */
    public double directorAffinityOf(String director) {
        return byNormalizedName(directorAffinity, director);
    }

    /**
     * Resolves a movie's director against the profile: by TMDB person id when it is
     * known and in the profile, otherwise by normalized name.
     */
    public double directorAffinityOf(Director director) {
        if (director == null) {
            return 0.0;
        }
        if (director.tmdbId() != null && directorIdToName.containsKey(director.tmdbId())) {
            return directorAffinity.getOrDefault(directorIdToName.get(director.tmdbId()), 0.0);
        }
        return byNormalizedName(directorAffinity, director.name());
    }

    public double genreAffinityOf(String genre) {
        return genre == null ? 0.0 : genreAffinity.getOrDefault(genre, 0.0);
    }

    public double actorAffinityOf(String actor) {
        return byNormalizedName(actorAffinity, actor);
    }

    public double actorAffinityOf(Actor actor) {
        if (actor == null) {
            return 0.0;
        }
        if (actor.tmdbId() != null && actorIdToName.containsKey(actor.tmdbId())) {
            return actorAffinity.getOrDefault(actorIdToName.get(actor.tmdbId()), 0.0);
        }
        return byNormalizedName(actorAffinity, actor.name());
    }

    /** The profile's display name for a movie's director, or the movie's own name. */
    public String directorDisplayName(Director director) {
        if (director == null) {
            return null;
        }
        if (director.tmdbId() != null && directorIdToName.containsKey(director.tmdbId())) {
            return directorIdToName.get(director.tmdbId());
        }
        return matchingKey(directorAffinity, director.name()).orElse(director.name());
    }

    public String actorDisplayName(Actor actor) {
        if (actor == null) {
            return null;
        }
        if (actor.tmdbId() != null && actorIdToName.containsKey(actor.tmdbId())) {
            return actorIdToName.get(actor.tmdbId());
        }
        return matchingKey(actorAffinity, actor.name()).orElse(actor.name());
    }

    public double countryAffinityOf(String country) {
        return country == null ? 0.0 : countryAffinity.getOrDefault(country, 0.0);
    }

    public double decadeAffinityOf(int decade) {
        return decadeAffinity.getOrDefault(decade, 0.0);
    }

    // --- lookup helpers -----------------------------------------------------

    private static double byNormalizedName(Map<String, Double> affinities, String name) {
        return matchingKey(affinities, name).map(affinities::get).orElse(0.0);
    }

    private static java.util.Optional<String> matchingKey(Map<String, Double> affinities, String name) {
        if (name == null || name.isBlank()) {
            return java.util.Optional.empty();
        }
        String norm = normalizeName(name);
        if (affinities.containsKey(name)) {
            return java.util.Optional.of(name);
        }
        return affinities.keySet().stream()
                .filter(k -> normalizeName(k).equals(norm))
                .findFirst();
    }

    static String normalizeName(String value) {
        return PersonKeys.normalizeName(value);
    }

    // --- construction --------------------------------------------------------------

    private static Map<String, Double> affinityMap(Map<String, Double> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> copy = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            if (key != null && !key.isBlank()) {
                copy.put(key.trim(), requireAffinity(value, key));
            }
        });
        return Map.copyOf(copy);
    }

    private static Map<Integer, Double> decadeAffinityMap(Map<Integer, Double> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Double> copy = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            if (key != null) {
                copy.put(key, requireAffinity(value, String.valueOf(key)));
            }
        });
        return Map.copyOf(copy);
    }

    private static double requireAffinity(Double value, String key) {
        if (value == null || !Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    "affinity for '" + key + "' must be within [0, 1]: " + value);
        }
        return value;
    }

    private static Map<String, Double> uniform(Set<String> values) {
        if (values == null) {
            return Map.of();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.toUnmodifiableMap(String::trim, v -> 1.0, (a, b) -> a));
    }

    private static Map<Integer, Double> uniformInts(Set<Integer> values) {
        if (values == null) {
            return Map.of();
        }
        return values.stream()
                .filter(v -> v != null)
                .collect(Collectors.toUnmodifiableMap(v -> v, v -> 1.0, (a, b) -> a));
    }
}
