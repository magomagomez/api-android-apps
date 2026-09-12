package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.scoring.PersonalAffinityCalculator;
import com.magomez.androidapps.movierec.scoring.PersonalAffinitySignals;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PersonalAffinityCalculator} — three dimensions only (director, genre, actor).
 * Country and decade were removed from the PERSONAL MATCH SCORE and are not computed.
 */
class PersonalAffinityCalculatorTest {

    private final PersonalAffinityCalculator calculator = new PersonalAffinityCalculator();

    private static Movie movie(Director director, List<Genre> genres, List<Actor> actors,
                               String releaseDate) {
        return new Movie(1, "tt1", "The Movie", "The Movie", releaseDate, 120, "overview",
                genres, director, actors, List.of(Country.of("Nowhere")), null, List.of());
    }

    private static UserTasteProfile profile(Set<String> directors, Set<String> genres,
                                            Set<String> actors) {
        return new UserTasteProfile(directors, genres, actors, Set.of(), Set.of());
    }

    private static UserTasteProfile weighted(Map<String, Double> directors, Map<String, Double> genres,
                                             Map<String, Double> actors) {
        return new UserTasteProfile(directors, genres, actors, Map.of(), Map.of());
    }

    // --- director -------------------------------------------------------------

    @Test
    void directorExactMatchScores100() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("Coralie Fargeat"), List.of(), List.of(), null),
                profile(Set.of("Coralie Fargeat"), Set.of(), Set.of()));

        assertThat(signals.directorAffinity()).isEqualTo(100.0);
    }

    @Test
    void directorNoMatchScores0() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("Denis Villeneuve"), List.of(), List.of(), null),
                profile(Set.of("Coralie Fargeat"), Set.of(), Set.of()));

        assertThat(signals.directorAffinity()).isEqualTo(0.0);
    }

    @Test
    void directorMissingOnTheMovieScores0() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(null, List.of(), List.of(), null),
                profile(Set.of("Coralie Fargeat"), Set.of(), Set.of()));

        assertThat(signals.directorAffinity()).isEqualTo(0.0);
    }

    // --- genres --------------------------------------------------------------

    @Test
    void genreAffinityIsThePercentageOfMovieGenresThatArePreferred() {
        Movie theMovie = movie(null, List.of(Genre.of("Terror"), Genre.of("Drama")), List.of(), null);

        assertThat(calculator.calculate(theMovie,
                profile(Set.of(), Set.of("Terror"), Set.of())).genreAffinity()).isEqualTo(50.0);

        assertThat(calculator.calculate(theMovie,
                profile(Set.of(), Set.of("Terror", "Drama", "Comedia"), Set.of()))
                .genreAffinity()).isEqualTo(100.0);
    }

    @Test
    void genreAffinityRoundsToOneDecimal() {
        Movie theMovie = movie(null,
                List.of(Genre.of("A"), Genre.of("B"), Genre.of("C")), List.of(), null);

        assertThat(calculator.calculate(theMovie,
                profile(Set.of(), Set.of("A"), Set.of())).genreAffinity()).isEqualTo(33.3);
    }

    @Test
    void genreAffinityIs0WhenTheMovieHasNoGenresOrThePreferenceIsEmpty() {
        assertThat(calculator.calculate(movie(null, List.of(), List.of(), null),
                profile(Set.of(), Set.of("Terror"), Set.of())).genreAffinity()).isEqualTo(0.0);

        assertThat(calculator.calculate(movie(null, List.of(Genre.of("Terror")), List.of(), null),
                UserTasteProfile.empty()).genreAffinity()).isEqualTo(0.0);
    }

    // --- actors -------------------------------------------------------------

    @Test
    void actorAffinityIsThePercentageOfMovieActorsThatArePreferred() {
        Movie theMovie = movie(null, List.of(),
                List.of(Actor.of("A"), Actor.of("B"), Actor.of("C"), Actor.of("D")), null);

        assertThat(calculator.calculate(theMovie,
                profile(Set.of(), Set.of(), Set.of("A"))).actorAffinity()).isEqualTo(25.0);

        assertThat(calculator.calculate(
                movie(null, List.of(), List.of(Actor.of("A"), Actor.of("B")), null),
                profile(Set.of(), Set.of(), Set.of("A", "B"))).actorAffinity()).isEqualTo(100.0);
    }

    // --- text comparison -------------------------------------------------

    @Test
    void textComparisonIsTrimmedAndCaseInsensitive() {
        Movie theMovie = movie(Director.of("CORALIE FARGEAT"),
                List.of(Genre.of("Terror")), List.of(Actor.of("Demi Moore")), null);
        UserTasteProfile messyCasingProfile = profile(
                Set.of("  coralie fargeat  "), Set.of("TERROR"), Set.of(" demi moore"));

        PersonalAffinitySignals signals = calculator.calculate(theMovie, messyCasingProfile);

        assertThat(signals.directorAffinity()).isEqualTo(100.0);
        assertThat(signals.genreAffinity()).isEqualTo(100.0);
        assertThat(signals.actorAffinity()).isEqualTo(100.0);
    }

    // --- empty inputs --------------------------------------------------

    @Test
    void movieWithoutAnyDataYieldsAllZeroSignals() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(null, List.of(), List.of(), null),
                profile(Set.of("Coralie Fargeat"), Set.of("Terror"), Set.of("Demi Moore")));

        assertThat(signals).isEqualTo(new PersonalAffinitySignals(0, 0, 0));
    }

    @Test
    void emptyProfileYieldsAllZeroSignals() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("Coralie Fargeat"), List.of(Genre.of("Terror")),
                        List.of(Actor.of("Demi Moore")), "2024-09-07"),
                UserTasteProfile.empty());

        assertThat(signals).isEqualTo(new PersonalAffinitySignals(0, 0, 0));
    }

    // --- invariants --------------------------------------------------

    @Test
    void everySignalStaysWithinZeroTo100() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("Coralie Fargeat"),
                        List.of(Genre.of("Terror"), Genre.of("Drama"), Genre.of("Comedia")),
                        List.of(Actor.of("A"), Actor.of("B"), Actor.of("C")), "2024-09-07"),
                profile(Set.of("Coralie Fargeat"), Set.of("Terror"), Set.of("A", "B")));

        assertThat(List.of(signals.directorAffinity(), signals.genreAffinity(), signals.actorAffinity()))
                .allSatisfy(v -> assertThat(v).isBetween(0.0, 100.0));
    }

    @Test
    void personalAffinitySignalsRejectsValuesOutsideZeroTo100() {
        assertThatThrownBy(() -> new PersonalAffinitySignals(101, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PersonalAffinitySignals(0, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotModifyTheMovie() {
        Director director = Director.of("Coralie Fargeat");
        List<Genre> genres = List.of(Genre.of("Terror"), Genre.of("Drama"));
        List<Actor> actors = List.of(Actor.of("Demi Moore"));
        Movie theMovie = movie(director, genres, actors, "2024-09-07");
        Movie snapshot = movie(director, genres, actors, "2024-09-07");

        calculator.calculate(theMovie,
                profile(Set.of("Coralie Fargeat"), Set.of("Terror"), Set.of("Demi Moore")));

        assertThat(theMovie).isEqualTo(snapshot);
        assertThat(theMovie.genres()).containsExactly(Genre.of("Terror"), Genre.of("Drama"));
        assertThat(theMovie.director()).isEqualTo(director);
    }

    @Test
    void rejectsNullArguments() {
        UserTasteProfile anyProfile = UserTasteProfile.empty();
        Movie anyMovie = movie(null, List.of(), List.of(), null);

        assertThatThrownBy(() -> calculator.calculate(null, anyProfile))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calculator.calculate(anyMovie, null))
                .isInstanceOf(NullPointerException.class);
    }

    // --- weighted affinities ([0,1] preferences -> [0,100] signals) --------------

    @Test
    void aPreferenceOfOnePointZeroYields100() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("Fincher"), List.of(Genre.of("Thriller")), List.of(), "2024-01-01"),
                weighted(Map.of("Fincher", 1.0), Map.of("Thriller", 1.0), Map.of()));

        assertThat(signals.directorAffinity()).isEqualTo(100.0);
        assertThat(signals.genreAffinity()).isEqualTo(100.0);
    }

    @Test
    void aPreferenceOfZeroPointFiveYields50() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("Fincher"), List.of(Genre.of("Thriller")), List.of(), "2024-01-01"),
                weighted(Map.of("Fincher", 0.5), Map.of("Thriller", 0.5), Map.of()));

        assertThat(signals.directorAffinity()).isEqualTo(50.0);
        assertThat(signals.genreAffinity()).isEqualTo(50.0);
    }

    @Test
    void directorUsesTheExactStoredWeight() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("Nolan"), List.of(), List.of(), "2014-11-07"),
                weighted(Map.of("Nolan", 0.3), Map.of(), Map.of()));

        assertThat(signals.directorAffinity()).isEqualTo(30.0);
    }

    @Test
    void aMovieWithNoMatchesYieldsAllZero() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("Unknown"), List.of(Genre.of("Western")), List.of(Actor.of("Nobody")),
                        "2024-01-01"),
                weighted(Map.of("Fincher", 1.0), Map.of("Thriller", 1.0), Map.of("Pitt", 1.0)));

        assertThat(signals).isEqualTo(new PersonalAffinitySignals(0, 0, 0));
    }

    @Test
    void severalMatchesAreCombinedByArithmeticMean() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(null, List.of(Genre.of("Thriller"), Genre.of("Drama")), List.of(), "2024-01-01"),
                weighted(Map.of(), Map.of("Thriller", 1.0, "Drama", 0.5), Map.of()));

        assertThat(signals.genreAffinity()).isEqualTo(75.0); // mean(1.0, 0.5) * 100
    }

    @Test
    void aValueTheUserDoesNotPreferCountsAsZeroInTheMean() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(null, List.of(Genre.of("Thriller"), Genre.of("Musical")), List.of(), "2024-01-01"),
                weighted(Map.of(), Map.of("Thriller", 1.0), Map.of()));

        assertThat(signals.genreAffinity()).isEqualTo(50.0); // mean(1.0, 0.0) * 100
    }

    @Test
    void addingMoreValuesWithEqualAffinityDoesNotRaiseTheScore() {
        Map<String, Double> genres3 = Map.of("A", 0.6, "B", 0.6, "C", 0.6);
        Map<String, Double> actors3 = Map.of("X", 0.6, "Y", 0.6, "Z", 0.6);

        double two = calculator.calculate(
                movie(null, List.of(Genre.of("A"), Genre.of("B")),
                        List.of(Actor.of("X"), Actor.of("Y")), "2024-01-01"),
                weighted(Map.of(), genres3, actors3)).genreAffinity();

        PersonalAffinitySignals three = calculator.calculate(
                movie(null, List.of(Genre.of("A"), Genre.of("B"), Genre.of("C")),
                        List.of(Actor.of("X"), Actor.of("Y"), Actor.of("Z")), "2024-01-01"),
                weighted(Map.of(), genres3, actors3));

        assertThat(two).isEqualTo(60.0);
        assertThat(three.genreAffinity()).isEqualTo(60.0);
        assertThat(three.actorAffinity()).isEqualTo(60.0);
    }

    @Test
    void weightLookupIsCaseInsensitive() {
        PersonalAffinitySignals signals = calculator.calculate(
                movie(Director.of("CHRISTOPHER NOLAN"), List.of(Genre.of("  sci-fi ")), List.of(), "2024-01-01"),
                weighted(Map.of("christopher nolan", 0.9), Map.of("Sci-Fi", 0.8), Map.of()));

        assertThat(signals.directorAffinity()).isEqualTo(90.0);
        assertThat(signals.genreAffinity()).isEqualTo(80.0);
    }

    @Test
    void isDeterministic() {
        Movie theMovie = movie(Director.of("Nolan"), List.of(Genre.of("Sci-Fi"), Genre.of("Drama")),
                List.of(Actor.of("Bale")), "2014-11-07");
        UserTasteProfile taste = weighted(Map.of("Nolan", 0.7), Map.of("Sci-Fi", 0.9, "Drama", 0.4),
                Map.of("Bale", 0.6));

        assertThat(calculator.calculate(theMovie, taste)).isEqualTo(calculator.calculate(theMovie, taste));
    }
}
