package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.model.SimilarMovie;
import com.magomez.androidapps.movierec.recommendation.SimilarityMatch;
import com.magomez.androidapps.movierec.recommendation.SimilaritySignal;
import com.magomez.androidapps.movierec.recommendation.SimilaritySignalCalculator;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibrary;
import com.magomez.androidapps.movierec.scoring.letterboxd.WatchedMovie;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SimilaritySignalCalculator}: "resembles films you have seen" — built only from
 * director / genre overlap, never from {@code genreAffinity}, and with a saturating bonus
 * so many matches do not dominate. Deterministic.
 */
class SimilaritySignalCalculatorTest {

    private final SimilaritySignalCalculator calculator = new SimilaritySignalCalculator();

    private static Movie candidate(String director, List<String> genres, SimilarMovie... similar) {
        return new Movie(1, "tt1", "Candidate", "Candidate", "2026-01-01", 120, "o",
                genres.stream().map(Genre::of).toList(), Director.of(director),
                List.of(Actor.of("A")), List.of(Country.of("USA")), null,
                List.of(new Rating("TMDB", 7.0, 1000)), List.of(similar));
    }

    private static WatchedMovie watched(int id, String title, String director, List<String> genres) {
        return new WatchedMovie(id, title, 8.0, genres, director);
    }

    private static LetterboxdLibrary library(WatchedMovie... watched) {
        return new LetterboxdLibrary(UserTasteProfile.empty(), List.of(watched));
    }

    @Test
    void noSimilarConnectionsYieldStrengthZero() {
        Movie m = candidate("Dir", List.of("Drama"),
                new SimilarMovie(50, "Not in history", 2000));
        SimilaritySignal signal = calculator.calculate(m, library(
                watched(99, "Something else", "Other", List.of("Comedia"))));

        assertThat(signal).isEqualTo(SimilaritySignal.none());
        assertThat(signal.similarityStrength()).isZero();
        assertThat(signal.matchCount()).isZero();
    }

    @Test
    void sharingOnlyOneNicheGenreIsNotAMatch() {
        Movie m = candidate("Dir", List.of("Terror", "Suspense"),
                new SimilarMovie(50, "One genre", 2000));
        SimilaritySignal signal = calculator.calculate(m, library(
                watched(50, "One genre", "Other Dir", List.of("Terror", "Comedia"))));

        assertThat(signal.hasMatches()).isFalse();
    }

    @Test
    void broadGenresLikeComedyOrDramaDoNotCountTowardsAMatch() {
        // both are "Comedia, Aventura, Acción" — that is not similarity
        Movie m = candidate("Dir", List.of("Comedia", "Aventura", "Accion"),
                new SimilarMovie(50, "Same broad tags", 2000));
        SimilaritySignal signal = calculator.calculate(m, library(
                watched(50, "Same broad tags", "Other", List.of("Comedia", "Aventura", "Accion"))));

        assertThat(signal.hasMatches()).isFalse();
    }

    @Test
    void twoSharedNicheGenresIsAMatchWithProportionalStrength() {
        // Drama is broad -> ignored; Crimen + Suspense + Misterio are niche
        Movie m = candidate("Dir", List.of("Crimen", "Suspense", "Misterio", "Drama"),
                new SimilarMovie(50, "Two", 2000), new SimilarMovie(51, "Three", 2001));
        SimilaritySignal signal = calculator.calculate(m, library(
                watched(50, "Two", "Other", List.of("Crimen", "Suspense", "Drama")),
                watched(51, "Three", "Other", List.of("Crimen", "Suspense", "Misterio"))));

        assertThat(signal.matchCount()).isEqualTo(2);
        SimilarityMatch two = signal.matches().stream().filter(x -> x.watchedTitle().equals("Two")).findFirst().orElseThrow();
        SimilarityMatch three = signal.matches().stream().filter(x -> x.watchedTitle().equals("Three")).findFirst().orElseThrow();
        assertThat(two.strength()).isEqualTo(0.4);  // 2 niche / 5
        assertThat(two.source()).isEqualTo(SimilarityMatch.Source.SHARED_GENRES);
        assertThat(two.sharedGenres()).containsExactlyInAnyOrder("Crimen", "Suspense");
        assertThat(three.strength()).isEqualTo(0.6); // 3 niche / 5
        assertThat(three.sharedGenres()).containsExactlyInAnyOrder("Crimen", "Suspense", "Misterio");
        assertThat(signal.similarityStrength()).isGreaterThan(0.6).isLessThanOrEqualTo(1.0);
    }

    @Test
    void sameDirectorIsAlwaysAFullStrengthMatch() {
        Movie m = candidate("Bong Joon-ho", List.of("Comedia"),
                new SimilarMovie(50, "Parasite", 2019));
        SimilaritySignal signal = calculator.calculate(m, library(
                watched(50, "Parasite", "bong joon-ho", List.of("Suspense")))); // no shared genre

        assertThat(signal.matchCount()).isEqualTo(1);
        SimilarityMatch match = signal.matches().get(0);
        assertThat(match.sameDirector()).isTrue();
        assertThat(match.strength()).isEqualTo(1.0);
        assertThat(match.source()).isEqualTo(SimilarityMatch.Source.SAME_DIRECTOR);
        assertThat(signal.similarityStrength()).isEqualTo(1.0);
    }

    @Test
    void manyMatchesRaiseTheStrengthButSaturate_tenIsNotTenTimesOne() {
        List<SimilarMovie> oneSimilar = List.of(new SimilarMovie(1, "W1", 2000));
        List<WatchedMovie> oneWatched = List.of(watched(1, "W1", "Other", List.of("Crimen", "Suspense")));

        List<SimilarMovie> tenSimilar = new ArrayList<>();
        List<WatchedMovie> tenWatched = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            tenSimilar.add(new SimilarMovie(i, "W" + i, 2000));
            tenWatched.add(watched(i, "W" + i, "Other", List.of("Crimen", "Suspense")));
        }

        Movie candOne = candidate("Dir", List.of("Crimen", "Suspense"), oneSimilar.toArray(SimilarMovie[]::new));
        Movie candTen = candidate("Dir", List.of("Crimen", "Suspense"), tenSimilar.toArray(SimilarMovie[]::new));

        double one = calculator.calculate(candOne, library(oneWatched.toArray(WatchedMovie[]::new)))
                .similarityStrength();
        double ten = calculator.calculate(candTen, library(tenWatched.toArray(WatchedMovie[]::new)))
                .similarityStrength();

        assertThat(one).isEqualTo(0.4);               // single 2-genre match
        assertThat(ten).isGreaterThan(one).isLessThanOrEqualTo(1.0);
        assertThat(ten).isLessThan(one * 3.0);        // nowhere near 10x
        assertThat(ten).isLessThanOrEqualTo(0.71);    // 0.4 + ~0.30 saturating bonus
    }

    @Test
    void similarityStrengthIgnoresGenreAffinity() {
        // profile "loves" every genre at 1.0; that must NOT change similarityStrength
        Movie m = candidate("Dir", List.of("Crimen", "Suspense"),
                new SimilarMovie(50, "W", 2000));
        WatchedMovie w = watched(50, "W", "Other", List.of("Crimen", "Suspense"));

        double withEmptyProfile = calculator.calculate(m,
                new LetterboxdLibrary(UserTasteProfile.empty(), List.of(w))).similarityStrength();
        double withGenreLover = calculator.calculate(m, new LetterboxdLibrary(
                new UserTasteProfile(java.util.Map.of(), java.util.Map.of("Crimen", 1.0, "Suspense", 1.0),
                        java.util.Map.of(), java.util.Map.of(), java.util.Map.of()),
                List.of(w))).similarityStrength();

        assertThat(withGenreLover).isEqualTo(withEmptyProfile);
    }

    @Test
    void isDeterministic() {
        Movie m = candidate("Dir", List.of("Crimen", "Suspense", "Drama"),
                new SimilarMovie(50, "B", 2000), new SimilarMovie(51, "A", 2001));
        LetterboxdLibrary lib = library(
                watched(50, "B", "Other", List.of("Crimen", "Suspense")),
                watched(51, "A", "Other", List.of("Crimen", "Suspense", "Drama")));

        assertThat(calculator.calculate(m, lib)).isEqualTo(calculator.calculate(m, lib));
    }
}
