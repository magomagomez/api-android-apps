package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.SimilarMovie;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibrary;
import com.magomez.androidapps.movierec.scoring.letterboxd.WatchedMovie;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Turns the candidate's TMDB "similar" list into a quantified {@link SimilaritySignal}.
 *
 * <p>It uses <b>only</b> the connections that already pass the current strong filter —
 * same director, or at least two shared genres — and does not fetch or invent anything.
 *
 * <h2>Formula (deterministic)</h2>
 * <p>Per match:
 * <ul>
 *   <li><b>same director</b> &rarr; {@code strength = 1.0} (source {@code SAME_DIRECTOR});</li>
 *   <li><b>&ge; 2 shared <em>niche</em> genres</b> &rarr;
 *       {@code strength = min(1.0, sharedNicheGenres / 5.0)} (source {@code SHARED_GENRES}):
 *       2&rarr;0.4, 3&rarr;0.6, 4&rarr;0.8, 5+&rarr;1.0. Only niche genres count
 *       (thriller/suspense, horror/terror, crime, mystery, sci-fi, war, western, history,
 *       documentary) &mdash; sharing broad tags like comedy, adventure, action or drama is
 *       not real similarity.</li>
 * </ul>
 * <p>Combined:
 * <pre>
 *   best  = max(match strengths)
 *   bonus = MULTI_MATCH_WEIGHT * (1 - e^(-(matchCount - 1) / MULTI_MATCH_SCALE))
 *   similarityStrength = round2( min(1.0, best + bonus) )
 * </pre>
 * <p>{@code best} dominates; {@code bonus} rewards extra matches but <b>saturates</b>
 * ({@code W = 0.30}, {@code scale = 2.0}): 1 match &rarr; +0, 2 &rarr; +0.12, 3 &rarr; +0.19,
 * 5 &rarr; +0.26, 10 &rarr; +0.30. A candidate with 10 matches is never worth 10&times; one.
 *
 * <p>{@code genreAffinity} is never consulted: this signal is "resembles films you have
 * seen", not "made of genres you like".
 */
@Component
public class SimilaritySignalCalculator {

    static final int MIN_SHARED_GENRES = 2;
    private static final double GENRE_STRENGTH_DIVISOR = 5.0;

    /**
     * Genres specific enough that sharing them means something. Broad tags (comedy,
     * adventure, action, drama, family, romance, animation, fantasy, music) are excluded:
     * "both are comedy-adventure" is not similarity.
     */
    private static final Set<String> NICHE_GENRES = Set.of(
            "suspense", "thriller", "terror", "horror", "crimen", "crime",
            "misterio", "mystery", "ciencia ficcion", "science fiction", "sci-fi",
            "belica", "war", "western", "documental", "documentary", "historia", "history");
    private static final double SAME_DIRECTOR_STRENGTH = 1.0;
    private static final double MULTI_MATCH_WEIGHT = 0.30;
    private static final double MULTI_MATCH_SCALE = 2.0;

    public SimilaritySignal calculate(Movie candidate, LetterboxdLibrary library) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(library, "library");

        String candidateDirector = candidate.director() == null ? null
                : normalize(candidate.director().name());
        List<String> candidateGenreNames = candidate.genres().stream()
                .map(Genre::name)
                .filter(n -> n != null && !n.isBlank())
                .toList();
        Set<String> candidateGenresNormalized = candidateGenreNames.stream()
                .map(SimilaritySignalCalculator::normalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<SimilarityMatch> matches = new ArrayList<>();
        for (SimilarMovie similar : candidate.similarMovies()) {
            Optional<WatchedMovie> watched = library.watchedMovie(similar.tmdbId());
            if (watched.isEmpty()) {
                continue;
            }
            toMatch(watched.get(), candidateDirector, candidateGenreNames, candidateGenresNormalized)
                    .ifPresent(matches::add);
        }

        matches.sort(Comparator.comparingDouble(SimilarityMatch::strength).reversed()
                .thenComparing(Comparator.comparingDouble(SimilarityMatch::watchedUserScore).reversed())
                .thenComparing(m -> m.watchedTitle() == null ? "" : m.watchedTitle()));

        if (matches.isEmpty()) {
            return SimilaritySignal.none();
        }
        double best = matches.stream().mapToDouble(SimilarityMatch::strength).max().orElse(0.0);
        double bonus = MULTI_MATCH_WEIGHT
                * (1.0 - Math.exp(-(matches.size() - 1) / MULTI_MATCH_SCALE));
        double strength = round2(Math.min(1.0, best + bonus));
        return new SimilaritySignal(strength, matches.size(), matches);
    }

    private static Optional<SimilarityMatch> toMatch(WatchedMovie watched, String candidateDirector,
                                                     List<String> candidateGenreNames,
                                                     Set<String> candidateGenresNormalized) {
        boolean sameDirector = candidateDirector != null && watched.directorName() != null
                && candidateDirector.equals(normalize(watched.directorName()));

        Set<String> watchedGenresNormalized = watched.genres().stream()
                .map(SimilaritySignalCalculator::normalize)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        // only niche genres count towards the "shares genres" match
        List<String> shared = candidateGenreNames.stream()
                .filter(g -> NICHE_GENRES.contains(normalize(g)))
                .filter(g -> watchedGenresNormalized.contains(normalize(g)))
                .distinct()
                .toList();

        if (sameDirector) {
            return Optional.of(new SimilarityMatch(watched.title(), watched.tmdbId(),
                    watched.userScore(), true, shared, SAME_DIRECTOR_STRENGTH,
                    SimilarityMatch.Source.SAME_DIRECTOR));
        }
        if (shared.size() >= MIN_SHARED_GENRES) {
            double strength = round2(Math.min(1.0, shared.size() / GENRE_STRENGTH_DIVISOR));
            return Optional.of(new SimilarityMatch(watched.title(), watched.tmdbId(),
                    watched.userScore(), false, shared, strength,
                    SimilarityMatch.Source.SHARED_GENRES));
        }
        return Optional.empty();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
