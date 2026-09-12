package com.magomez.androidapps.movierec.scoring.pattern;

import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.scoring.PersonKeys;
import com.magomez.androidapps.movierec.scoring.RatedMovie;
import com.magomez.androidapps.movierec.scoring.TastePattern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministically derives the user's {@link TastePattern}s from their rated movies.
 *
 * <p>The user's <b>well-liked</b> films define a pattern ({@code userScore >= 7.0} — on
 * this user's scale a 7 already means "genuinely good, worth it"). A pattern is produced
 * only when at least {@code MIN_SUPPORT} of those favourites fit the signature and its
 * computed {@code strength >= MIN_STRENGTH}.
 *
 * <p><b>strength</b> in {@code [0, 1]} is the stronger of two readings:
 * <pre>
 *   favFraction = matchingFavourites / totalFavourites
 *   allFraction = matchingRated      / totalRated
 *
 *   liftScore       = clamp( (min(favFraction/allFraction, 3) - 1) / 2 , 0, 1 )
 *   prevalenceScore = clamp( (favFraction - 0.03) / 0.15 , 0, 1 )
 *   strength        = round2( max(liftScore, prevalenceScore) )
 * </pre>
 * <ul>
 *   <li><b>lift</b> rewards a <em>distinctive</em> preference — a kind of film that rises
 *       to the top of the user's ratings disproportionately (Korean thriller: small, but
 *       clearly over-represented);</li>
 *   <li><b>prevalence</b> rewards a kind of film the user <em>deliberately watches a lot
 *       of</em> — a large share of their good films — even when its lift is close to 1
 *       (comedy-horror: watched constantly, rated fine).</li>
 * </ul>
 *
 * <p>{@code strength} takes the better of the two so a pattern is not invisible just
 * because it is common rather than distinctive. But {@code liftScore} alone — never
 * boosted by prevalence — becomes {@link TastePattern#preferenceStrength()}: a pattern
 * that is only common (lift ≈ 1, e.g. a genre combo that is exactly as frequent among the
 * user's favourites as everywhere else) still shows up and gets explained, but it does not
 * buy the same PERSONAL MATCH SCORE bonus as one the user actually prefers over their
 * baseline.
 *
 * <p>Every pattern carries transparent {@code indicators}. No LLM, no invented
 * preferences, no persistence. Country is only a defining trait of a pattern (Korean
 * thriller), never a scoring input.
 */
public class TastePatternDetector {

    static final double FAVOURITE_SCORE = 7.0;
    static final int MIN_SUPPORT = 4;
    static final double MIN_STRENGTH = 0.10;

    private static final double LIFT_FLOOR = 1.0;
    private static final double LIFT_CEILING = 3.0;
    /** Lift used when a pattern appears only among favourites and nowhere else. */
    private static final double LIFT_WHEN_FAVOURITES_ONLY = LIFT_CEILING;

    /** A kind of film below this share of the user's well-liked list carries no prevalence weight. */
    private static final double PREVALENCE_FLOOR = 0.03;
    /** Share of the well-liked list at which prevalence alone saturates strength (floor + span). */
    private static final double PREVALENCE_SPAN = 0.15;

    private static final int MAX_SUPPORTING_DIRECTORS = 5;
    private static final int MAX_SUPPORTING_MOVIES = 8;
    private static final int MAX_SUPPORTING_GENRES = 6;

    public List<TastePattern> detect(List<RatedMovie> ratedMovies) {
        Objects.requireNonNull(ratedMovies, "ratedMovies");

        List<RatedMovie> valid = ratedMovies.stream()
                .filter(r -> r != null && r.movie() != null)
                .toList();
        List<RatedMovie> favourites = valid.stream()
                .filter(r -> r.userScore() >= FAVOURITE_SCORE)
                .toList();
        if (favourites.isEmpty()) {
            return List.of();
        }

        List<TastePattern> patterns = new ArrayList<>();
        for (NarrativePattern definition : NarrativePattern.values()) {
            detectOne(definition, favourites, valid).ifPresent(patterns::add);
        }
        patterns.sort(Comparator.comparingDouble(TastePattern::strength).reversed()
                .thenComparing(TastePattern::id));
        return List.copyOf(patterns);
    }

    private Optional<TastePattern> detectOne(NarrativePattern definition,
                                             List<RatedMovie> favourites, List<RatedMovie> allRated) {
        List<RatedMovie> matching = favourites.stream()
                .filter(r -> fits(definition, r))
                .sorted(Comparator.comparingDouble(RatedMovie::userScore).reversed()
                        .thenComparing(r -> title(r.movie())))
                .toList();

        int matchCount = matching.size();
        if (matchCount < MIN_SUPPORT) {
            return Optional.empty();
        }

        long matchingRated = allRated.stream().filter(r -> fits(definition, r)).count();

        double favFraction = (double) matchCount / favourites.size();
        double allFraction = (double) matchingRated / allRated.size();
        double lift = allFraction > 0.0 ? favFraction / allFraction : LIFT_WHEN_FAVOURITES_ONLY;

        double liftScore = clamp01(
                (Math.min(lift, LIFT_CEILING) - LIFT_FLOOR) / (LIFT_CEILING - LIFT_FLOOR));
        double prevalenceScore = clamp01((favFraction - PREVALENCE_FLOOR) / PREVALENCE_SPAN);
        double strength = round2(Math.max(liftScore, prevalenceScore));
        if (strength < MIN_STRENGTH) {
            return Optional.empty();
        }

        List<String> supportingGenres = byFrequency(matching.stream()
                .flatMap(r -> definition.triggerGenresIn(genreNames(r.movie())).stream())
                .toList()).stream().limit(MAX_SUPPORTING_GENRES).toList();
        List<String> supportingDirectors = topDirectors(matching);
        List<String> supportingMovies = matching.stream()
                .map(r -> title(r.movie()))
                .filter(t -> !t.isBlank())
                .distinct()
                .limit(MAX_SUPPORTING_MOVIES)
                .toList();

        List<String> indicators = indicators(matchCount, favourites.size(), favFraction, lift,
                liftScore, prevalenceScore, supportingGenres, supportingDirectors, matching);

        return Optional.of(new TastePattern(
                definition.id(), definition.displayName(), definition.description(), strength,
                liftScore, supportingGenres, supportingDirectors, supportingMovies, indicators));
    }

    private static boolean fits(NarrativePattern definition, RatedMovie rated) {
        return definition.matchesRaw(genreNames(rated.movie()), countryNames(rated.movie()));
    }

    /** Recurring directors of the matching favourites, grouped by TMDB person id, display name kept. */
    private static List<String> topDirectors(List<RatedMovie> matching) {
        Map<String, Double> weightByKey = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> namesByKey = new LinkedHashMap<>();
        for (RatedMovie r : matching) {
            Director d = r.movie().director();
            if (d == null || d.name() == null || d.name().isBlank()) {
                continue;
            }
            String key = PersonKeys.identityKey(d.tmdbId(), d.name());
            weightByKey.merge(key, 1.0, Double::sum);
            namesByKey.computeIfAbsent(key, k -> new LinkedHashMap<>()).merge(d.name().trim(), 1, Integer::sum);
        }
        return weightByKey.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(e -> PersonKeys.chooseDisplayName(namesByKey.get(e.getKey())))
                .distinct()
                .limit(MAX_SUPPORTING_DIRECTORS)
                .toList();
    }

    /** Tangible, qualitative — no raw percentages or internal scores in the wording. */
    private static List<String> indicators(int matchCount, int favourites, double favFraction,
                                           double lift, double liftScore, double prevalenceScore,
                                           List<String> genres, List<String> directors,
                                           List<RatedMovie> matching) {
        List<String> lines = new ArrayList<>();
        lines.add(prevalenceDescription(favFraction));
        if (!genres.isEmpty()) {
            lines.add("géneros que lo sustentan: " + String.join(", ", genres));
        }
        if (!directors.isEmpty()) {
            lines.add("directores recurrentes: " + String.join(", ", directors));
        }
        String examples = matching.stream()
                .limit(3)
                .map(r -> title(r.movie()) + " (" + trimScore(r.userScore()) + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        if (!examples.isBlank()) {
            lines.add("ejemplos: " + examples);
        }
        lines.add(characterization(lift, liftScore, prevalenceScore));
        return lines;
    }

    private static String prevalenceDescription(double favFraction) {
        if (favFraction >= 0.10) {
            return "es uno de los tipos de película que más se repiten en tu historial";
        }
        if (favFraction >= 0.05) {
            return "aparece de forma habitual en tu historial";
        }
        return "aparece de forma puntual, pero clara, en tu historial";
    }

    private static String characterization(double lift, double liftScore, double prevalenceScore) {
        if (liftScore >= prevalenceScore && lift > 1.0) {
            return "cuando ves este tipo de película, sueles valorarla muy por encima de tu media habitual";
        }
        return "es un tipo de película que ves —y valoras bien— con mucha frecuencia";
    }

    // --- helpers -----------------------------------------------------------

    private static List<String> genreNames(Movie movie) {
        return movie.genres().stream().map(Genre::name).filter(Objects::nonNull).toList();
    }

    private static List<String> countryNames(Movie movie) {
        return movie.countries().stream().map(Country::name).filter(Objects::nonNull).toList();
    }

    private static String title(Movie movie) {
        return movie.title() == null ? "" : movie.title();
    }

    /** Distinct values ordered by descending frequency, ties broken alphabetically. */
    private static List<String> byFrequency(List<String> values) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String v : values) {
            counts.merge(v, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String trimScore(double score) {
        return score == Math.rint(score) ? String.valueOf((long) score) : String.valueOf(score);
    }
}
