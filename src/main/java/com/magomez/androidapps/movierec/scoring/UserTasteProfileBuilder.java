package com.magomez.androidapps.movierec.scoring;

import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.scoring.pattern.TastePatternDetector;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Deterministic construction of a weighted {@link UserTasteProfile} from the movies the
 * user has rated ({@link RatedMovie}).
 *
 * <p>Only movies with {@code userScore >= 7.0} contribute. Each contributes a weight
 * {@code 1 + (userScore - 7.0)}: a {@code 7.0} favourite weighs {@code 1.0}, a
 * {@code 10.0} favourite weighs {@code 4.0}. Per dimension the weights of a value are
 * summed, then the dimension is normalized so its strongest value is {@code 1.0}.
 *
 * <p>Directors and actors are grouped by their <b>TMDB person id</b> (falling back to a
 * normalized name when the id is missing), so the same person credited under different
 * name spellings ({@code "Bong Joon-ho"} / {@code "봉준호"}) counts once. The display name
 * kept for that person prefers a Latin-script spelling.
 *
 * <p>Genre weight is discounted by <b>lift</b> — the same measure
 * {@link TastePatternDetector} uses for narrative patterns: how much more often a genre
 * appears among favourites than across everything rated. A near-universal tag like
 * "Drama" is typically about as common among favourites as in the rest of the library
 * (lift &asymp; 1) — it says little about this user's actual taste, so it is discounted
 * toward {@code 0} instead of dominating {@code genreAffinity} on raw favourite count
 * alone. A genre that is genuinely over-represented among favourites keeps its weight.
 *
 * <p>Fully deterministic, reproducible; no LLM, no persistence. Original {@link Movie}s
 * and their ratings are never modified.
 */
@Component
public class UserTasteProfileBuilder {

    static final double FAVOURITE_THRESHOLD = 7.0;

    /** Genre lift at/below this value is treated as "no real preference" (see class doc). */
    private static final double GENRE_LIFT_FLOOR = 1.0;
    /** Genre lift at/above this value gets the full, undiscounted weight. */
    private static final double GENRE_LIFT_CEILING = 3.0;

    private final TastePatternDetector tastePatternDetector = new TastePatternDetector();

    public UserTasteProfile build(List<RatedMovie> ratedMovies) {
        Objects.requireNonNull(ratedMovies, "ratedMovies");

        List<RatedMovie> valid = ratedMovies.stream().filter(Objects::nonNull).toList();
        List<RatedMovie> favourites = valid.stream()
                .filter(r -> r.userScore() >= FAVOURITE_THRESHOLD)
                .toList();

        PersonAffinities directors = new PersonAffinities();
        PersonAffinities actors = new PersonAffinities();
        Map<String, Double> genreWeights = new LinkedHashMap<>();
        Map<String, Integer> genreFavouriteCounts = new LinkedHashMap<>();
        Map<String, Integer> genreAllCounts = new LinkedHashMap<>();
        Map<String, Double> countries = new LinkedHashMap<>();
        Map<Integer, Double> decades = new LinkedHashMap<>();

        for (RatedMovie rated : valid) {
            rated.movie().genres().forEach(g -> addCount(genreAllCounts, g.name()));
        }

        for (RatedMovie rated : favourites) {
            Movie movie = rated.movie();
            double weight = weightOf(rated.userScore());

            if (movie.director() != null) {
                directors.add(movie.director().tmdbId(), movie.director().name(), weight);
            }
            movie.actors().forEach(a -> actors.add(a.tmdbId(), a.name(), weight));
            movie.genres().forEach(g -> {
                addWeight(genreWeights, g.name(), weight);
                addCount(genreFavouriteCounts, g.name());
            });
            movie.countries().forEach(c -> addWeight(countries, c.name(), weight));

            OptionalInt decade = Decades.of(movie.releaseDate());
            if (decade.isPresent()) {
                decades.merge(decade.getAsInt(), weight, Double::sum);
            }
        }

        Map<String, Double> genres = discountGenericGenres(
                genreWeights, genreFavouriteCounts, genreAllCounts, favourites.size(), valid.size());

        PersonAffinities.Result directorResult = directors.normalized();
        PersonAffinities.Result actorResult = actors.normalized();

        return new UserTasteProfile(
                directorResult.affinity(), normalize(genres), actorResult.affinity(),
                normalize(countries), normalizeInts(decades),
                tastePatternDetector.detect(ratedMovies),
                directorResult.idToName(), actorResult.idToName());
    }

    /**
     * Multiplies each genre's weighted favourite score by a {@code [0, 1]} lift score:
     * {@code 0} at lift {@code <= GENRE_LIFT_FLOOR} (no more common among favourites than
     * across the whole library — not a real preference), {@code 1} at lift
     * {@code >= GENRE_LIFT_CEILING}, linear in between. A genre absent from the rest of the
     * library entirely (lift undefined, division by zero) is treated as maximally
     * distinctive rather than penalised for sparse data.
     *
     * <p>Skipped entirely when there is no non-favourite rating at all ({@code totalRated
     * <= totalFavourites}): with nothing outside the favourites to compare against, lift
     * cannot be computed, and every genre would otherwise come out at exactly {@code 1.0}
     * and be wrongly zeroed out for "looking generic" rather than for actually being so.
     */
    private static Map<String, Double> discountGenericGenres(Map<String, Double> weighted,
            Map<String, Integer> favouriteCounts, Map<String, Integer> allCounts,
            int totalFavourites, int totalRated) {
        if (weighted.isEmpty() || totalFavourites == 0 || totalRated <= totalFavourites) {
            return weighted;
        }
        Map<String, Double> discounted = new LinkedHashMap<>();
        weighted.forEach((genre, weight) -> {
            double favFraction = favouriteCounts.getOrDefault(genre, 0) / (double) totalFavourites;
            double allFraction = allCounts.getOrDefault(genre, 0) / (double) totalRated;
            double lift = allFraction > 0.0 ? favFraction / allFraction : GENRE_LIFT_CEILING;
            double liftScore = clamp01((Math.min(lift, GENRE_LIFT_CEILING) - GENRE_LIFT_FLOOR)
                    / (GENRE_LIFT_CEILING - GENRE_LIFT_FLOOR));
            discounted.put(genre, weight * liftScore);
        });
        return discounted;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static void addCount(Map<String, Integer> target, String value) {
        if (value != null && !value.isBlank()) {
            target.merge(value.trim(), 1, Integer::sum);
        }
    }

    /** Per-favourite weight: a {@code 7.0} rating weighs {@code 1.0}, a {@code 10.0} weighs {@code 4.0}. */
    static double weightOf(double userScore) {
        return 1.0 + (userScore - FAVOURITE_THRESHOLD);
    }

    private static void addWeight(Map<String, Double> target, String value, double weight) {
        if (value != null && !value.isBlank()) {
            target.merge(value.trim(), weight, Double::sum);
        }
    }

    private static Map<String, Double> normalize(Map<String, Double> raw) {
        if (raw.isEmpty()) {
            return Map.of();
        }
        double max = raw.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        Map<String, Double> out = new LinkedHashMap<>();
        raw.forEach((key, value) -> out.put(key, round(value / max)));
        return out;
    }

    private static Map<Integer, Double> normalizeInts(Map<Integer, Double> raw) {
        if (raw.isEmpty()) {
            return Map.of();
        }
        double max = raw.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        Map<Integer, Double> out = new LinkedHashMap<>();
        raw.forEach((key, value) -> out.put(key, round(value / max)));
        return out;
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }

    /**
     * Accumulates person weights keyed by stable identity (TMDB person id when present,
     * else normalized name), tracking every name spelling seen so a readable display name
     * can be chosen. Deterministic.
     */
    static final class PersonAffinities {

        private final Map<String, Double> weightByKey = new LinkedHashMap<>();
        private final Map<String, Map<String, Integer>> nameCountsByKey = new LinkedHashMap<>();
        private final Map<String, Integer> personIdByKey = new LinkedHashMap<>();

        void add(Integer personId, String rawName, double weight) {
            if (rawName == null || rawName.isBlank()) {
                return;
            }
            String name = rawName.trim();
            String key = PersonKeys.identityKey(personId, name);
            weightByKey.merge(key, weight, Double::sum);
            nameCountsByKey.computeIfAbsent(key, k -> new LinkedHashMap<>()).merge(name, 1, Integer::sum);
            if (personId != null) {
                personIdByKey.put(key, personId);
            }
        }

        Result normalized() {
            if (weightByKey.isEmpty()) {
                return new Result(Map.of(), Map.of());
            }
            Map<String, String> displayByKey = new LinkedHashMap<>();
            nameCountsByKey.forEach((key, counts) -> displayByKey.put(key, PersonKeys.chooseDisplayName(counts)));

            Map<String, Double> weightByDisplay = new LinkedHashMap<>();
            weightByKey.forEach((key, w) ->
                    weightByDisplay.merge(displayByKey.get(key), w, Double::sum));

            double max = weightByDisplay.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            Map<String, Double> affinity = new LinkedHashMap<>();
            weightByDisplay.forEach((name, w) -> affinity.put(name, round(w / max)));

            Map<Integer, String> idToName = new LinkedHashMap<>();
            personIdByKey.forEach((key, id) -> idToName.put(id, displayByKey.get(key)));

            return new Result(affinity, idToName);
        }

        record Result(Map<String, Double> affinity, Map<Integer, String> idToName) {
        }
    }
}
