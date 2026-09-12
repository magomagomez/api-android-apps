package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.scoring.RatedMovie;
import com.magomez.androidapps.movierec.scoring.TastePattern;
import com.magomez.androidapps.movierec.scoring.pattern.TastePatternDetector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TastePatternDetector}: strength is a <em>relative preference intensity</em>
 * (lift of the pattern among the best-rated films vs. among everything rated), it stays
 * in {@code [0, 1]}, it does not saturate just because there are many matches, and a
 * small but concentrated niche is not penalised. Deterministic throughout.
 */
class TastePatternDetectorTest {

    private final TastePatternDetector detector = new TastePatternDetector();
    private int seq = 0;

    private Movie movie(String director, List<String> genres, String country) {
        int id = ++seq;
        return new Movie(id, "tt" + id, "M" + id, "M" + id, "2018-01-01", 120, "overview",
                genres.stream().map(Genre::of).toList(), Director.of(director),
                List.of(Actor.of("Actor " + id)), List.of(Country.of(country)), null, List.of());
    }

    private RatedMovie kt(double score) {
        return new RatedMovie(movie(seq % 2 == 0 ? "Bong Joon-ho" : "Park Chan-wook",
                List.of("Suspense", "Crimen"), "Corea del Sur"), score);
    }

    private RatedMovie other(double score) {
        return new RatedMovie(movie("Filler " + seq, List.of("Aventura"),
                "Estados Unidos de América"), score);
    }

    private List<RatedMovie> list(int ktFav, int ktNonFav, int otherFav, int otherNonFav) {
        List<RatedMovie> l = new ArrayList<>();
        for (int i = 0; i < ktFav; i++) l.add(kt(9.0));
        for (int i = 0; i < ktNonFav; i++) l.add(kt(6.0));
        for (int i = 0; i < otherFav; i++) l.add(other(9.0));
        for (int i = 0; i < otherNonFav; i++) l.add(other(6.0));
        return l;
    }

    private static Optional<TastePattern> pattern(List<TastePattern> patterns, String id) {
        return patterns.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    // --- detection & transparency ------------------------------------------

    @Test
    void detectsAPatternThatIsOverRepresentedAmongFavourites() {
        // 8 Korean thrillers, all favourites, none elsewhere; lots of unrelated non-favourites
        List<RatedMovie> history = list(8, 0, 12, 60);

        List<TastePattern> patterns = detector.detect(history);

        assertThat(pattern(patterns, "KOREAN_THRILLER")).hasValueSatisfying(p -> {
            assertThat(p.name()).isEqualTo("Thriller coreano");
            assertThat(p.strength()).isBetween(0.0, 1.0).isGreaterThan(0.6);
            assertThat(p.supportingGenres()).contains("Suspense", "Crimen");
            assertThat(p.supportingDirectors()).contains("Bong Joon-ho", "Park Chan-wook");
            assertThat(p.supportingMovies()).isNotEmpty();
            assertThat(p.indicators()).anyMatch(i -> i.contains("historial")); // qualitative, no raw stats
            assertThat(p.indicators()).anyMatch(i -> i.startsWith("géneros que lo sustentan:"));
            assertThat(p.indicators()).anyMatch(i -> i.startsWith("directores recurrentes:"));
            assertThat(p.indicators()).anyMatch(i -> i.startsWith("ejemplos:"));
            assertThat(p.indicators()).anyMatch(i -> i.contains("valorarla") || i.contains("valoras bien"));
            assertThat(p.indicators()).noneMatch(i -> i.matches(".*\\d.*%.*") || i.contains("liftScore"));
        });
    }

    @Test
    void noPatternWhenLiftIsFlatAndPrevalenceIsTiny() {
        // KT is ~2% of favourites, same rate everywhere -> liftScore 0, prevalenceScore 0
        List<RatedMovie> history = list(4, 12, 200, 600);

        assertThat(pattern(detector.detect(history), "KOREAN_THRILLER")).isEmpty();
    }

    @Test
    void aLowVolumeButHighlyConcentratedNicheIsNotPenalised() {
        // only 4 matching favourites, but they never appear outside favourites
        List<RatedMovie> history = list(4, 0, 16, 200);

        assertThat(pattern(detector.detect(history), "KOREAN_THRILLER")).hasValueSatisfying(p ->
                assertThat(p.strength()).isGreaterThan(0.7));
    }

    @Test
    void doesNotCreateAPatternWithoutEnoughSupport() {
        // only 3 matching favourites -> below MIN_SUPPORT
        assertThat(pattern(detector.detect(list(3, 0, 20, 60)), "KOREAN_THRILLER")).isEmpty();
    }

    @Test
    void onlyWellLikedMoviesCount() {
        List<RatedMovie> lowish = new ArrayList<>(list(3, 0, 20, 60));
        lowish.add(new RatedMovie(movie("Na Hong-jin", List.of("Suspense", "Terror"),
                "Corea del Sur"), 6.5)); // below the 7.0 bar -> not a favourite
        assertThat(pattern(detector.detect(lowish), "KOREAN_THRILLER")).isEmpty();

        List<RatedMovie> enough = new ArrayList<>(list(3, 0, 20, 60));
        enough.add(new RatedMovie(movie("Na Hong-jin", List.of("Suspense", "Terror"),
                "Corea del Sur"), 7.0)); // a 7 is "worth it" -> now a favourite, 4 matches
        assertThat(pattern(detector.detect(enough), "KOREAN_THRILLER")).isPresent();
    }

    @Test
    void noFavouritesMeansNoPatterns() {
        assertThat(detector.detect(list(0, 20, 0, 20))).isEmpty();
    }

    // --- strength semantics ---------------------------------------------

    @Test
    void withASmallShareTheLiftDrivesTheStrength() {
        // KT is a tiny share of favourites in both -> prevalence ~0; lift decides
        // "leaky": KT also present outside favourites -> lower lift
        double leaky = pattern(detector.detect(list(8, 6, 200, 400)), "KOREAN_THRILLER")
                .orElseThrow().strength();
        // "concentrated": KT almost absent outside favourites -> higher lift
        double concentrated = pattern(detector.detect(list(8, 0, 200, 400)), "KOREAN_THRILLER")
                .orElseThrow().strength();

        assertThat(concentrated).isGreaterThan(leaky);
        assertThat(concentrated).isLessThanOrEqualTo(1.0);
    }

    @Test
    void prevalenceIsAShareOfTheFavouritesNotARawCount() {
        // both: KT is 12% of the favourites -> same strength, whatever the volume
        double few = pattern(detector.detect(list(12, 0, 88, 0)), "KOREAN_THRILLER")
                .orElseThrow().strength();
        double many = pattern(detector.detect(list(24, 0, 176, 0)), "KOREAN_THRILLER")
                .orElseThrow().strength();

        assertThat(few).isEqualTo(many).isBetween(0.4, 0.8);
    }

    @Test
    void preferenceStrengthIsThePureLiftReadingNeverBoostedByPrevalence() {
        // KT is exactly as common among favourites as among everything rated (lift == 1):
        // no real preference, just volume. strength still picks it up via prevalence
        // (so it's not invisible), but preferenceStrength must not pretend it's distinctive.
        TastePattern kt = pattern(detector.detect(list(12, 0, 88, 0)), "KOREAN_THRILLER")
                .orElseThrow();

        assertThat(kt.strength()).isGreaterThan(0.4); // prevalence still surfaces the pattern
        assertThat(kt.preferenceStrength()).isZero(); // but there is no real lift behind it
    }

    @Test
    void strengthNeverLeavesZeroToOne() {
        detector.detect(list(100, 1, 20, 300))
                .forEach(p -> assertThat(p.strength()).isBetween(0.0, 1.0));
    }

    // --- UNCOMFORTABLE_CINEMA (recalculated after the signature change) -----

    @Test
    void detectsUncomfortableCinemaFromDramaPlusTerrorOrSuspenseFilms() {
        List<RatedMovie> history = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            history.add(new RatedMovie(movie("D " + i, List.of("Drama", "Suspense"), "USA"), 9.0));
        }
        history.addAll(list(0, 0, 0, 80)); // non-matching non-favourites -> over-representation

        assertThat(pattern(detector.detect(history), "UNCOMFORTABLE_CINEMA")).hasValueSatisfying(p -> {
            assertThat(p.strength()).isBetween(0.0, 1.0).isGreaterThan(0.3);
            assertThat(p.supportingGenres()).contains("Drama", "Suspense");
        });
    }

    @Test
    void detectsBlackComedyFromComedyPlusHorrorFilms() {
        List<RatedMovie> history = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            history.add(new RatedMovie(movie("D " + i, List.of("Comedia", "Terror"), "USA"), 9.0));
        }
        history.addAll(list(0, 0, 0, 80));

        assertThat(pattern(detector.detect(history), "BLACK_COMEDY")).hasValueSatisfying(p ->
                assertThat(p.strength()).isBetween(0.0, 1.0).isGreaterThan(0.3));
    }

    @Test
    void doesNotCountComedyPlusCrimeAsBlackComedy() {
        List<RatedMovie> history = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            history.add(new RatedMovie(movie("D " + i, List.of("Comedia", "Crimen"), "USA"), 9.0));
        }
        history.addAll(list(0, 0, 0, 80));

        assertThat(pattern(detector.detect(history), "BLACK_COMEDY")).isEmpty();
    }

    @Test
    void doesNotCountDramaPlusActionAsUncomfortableCinema() {
        List<RatedMovie> history = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            history.add(new RatedMovie(movie("A " + i, List.of("Drama", "Suspense", "Acción"), "USA"), 9.0));
        }
        history.addAll(list(0, 0, 0, 80));

        assertThat(pattern(detector.detect(history), "UNCOMFORTABLE_CINEMA")).isEmpty();
    }

    // --- determinism --------------------------------------------------

    @Test
    void isDeterministicAndSortedByStrength() {
        List<RatedMovie> history = new ArrayList<>(list(8, 0, 10, 80));
        for (int i = 0; i < 6; i++) {
            history.add(new RatedMovie(movie("Yorgos Lanthimos", List.of("Comedia", "Terror"),
                    "Grecia"), 9.0)); // black comedy favourites (comedy + horror)
        }

        assertThat(detector.detect(history)).isEqualTo(detector.detect(history));

        List<TastePattern> patterns = detector.detect(history);
        assertThat(patterns).hasSizeGreaterThanOrEqualTo(2);
        for (int i = 1; i < patterns.size(); i++) {
            assertThat(patterns.get(i - 1).strength())
                    .isGreaterThanOrEqualTo(patterns.get(i).strength());
        }
    }
}
