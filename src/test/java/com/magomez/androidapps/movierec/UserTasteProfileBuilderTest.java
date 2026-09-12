package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.scoring.RatedMovie;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.UserTasteProfileBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class UserTasteProfileBuilderTest {

    private final UserTasteProfileBuilder builder = new UserTasteProfileBuilder();

    private static Movie movie(Director director, List<Genre> genres, List<Actor> actors,
                               List<Country> countries, String releaseDate, List<Rating> ratings) {
        return new Movie(1, "tt1", "The Movie", "The Movie", releaseDate, 120, "overview",
                genres, director, actors, countries, null, ratings);
    }

    private static Movie fullMovie(String director, String genre, String actor, String country,
                                   String releaseDate) {
        return movie(Director.of(director), List.of(Genre.of(genre)), List.of(Actor.of(actor)),
                List.of(Country.of(country)), releaseDate, List.of());
    }

    @Test
    void noMoviesYieldsAnEmptyProfile() {
        assertThat(builder.build(List.of())).isEqualTo(UserTasteProfile.empty());
    }

    // --- person identity by TMDB person id -----------------------------------

    @Test
    void sameDirectorPersonIdWithDifferentNameSpellingsCountsOnce() {
        Movie a = movie(new Director(21684, "봉준호"), List.of(Genre.of("Suspense")),
                List.of(), List.of(Country.of("South Korea")), "2019-05-30", List.of());
        Movie b = movie(new Director(21684, "Bong Joon-ho"), List.of(Genre.of("Crimen")),
                List.of(), List.of(Country.of("South Korea")), "2003-04-25", List.of());
        Movie c = movie(new Director(999, "Someone Else"), List.of(Genre.of("Drama")),
                List.of(), List.of(Country.of("USA")), "2010-01-01", List.of());

        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(a, 9.0), new RatedMovie(b, 9.0), new RatedMovie(c, 8.0)));

        // one grouped entry, kept under the Latin-script spelling
        assertThat(profile.preferredDirectors()).contains("Bong Joon-ho").doesNotContain("봉준호");
        // both spellings resolve to the same (strongest) affinity via the person id
        assertThat(profile.directorAffinityOf(new Director(21684, "봉준호"))).isEqualTo(1.0);
        assertThat(profile.directorAffinityOf(new Director(21684, "Bong Joon-ho"))).isEqualTo(1.0);
        assertThat(profile.directorAffinityOf(new Director(999, "Someone Else"))).isLessThan(1.0);
        assertThat(profile.directorIdToName()).containsEntry(21684, "Bong Joon-ho");
    }

    @Test
    void directorsWithoutPersonIdFallBackToNameGrouping() {
        Movie a = fullMovie("Denis Villeneuve", "Sci-Fi", "A", "Canada", "2021-01-01");
        Movie b = fullMovie("  denis villeneuve ", "Drama", "B", "Canada", "2016-01-01");

        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(a, 9.0), new RatedMovie(b, 9.0)));

        assertThat(profile.preferredDirectors()).containsExactly("Denis Villeneuve");
        assertThat(profile.directorAffinityOf(Director.of("DENIS VILLENEUVE"))).isEqualTo(1.0);
        assertThat(profile.directorIdToName()).isEmpty();
    }

    @Test
    void personIdKeyingAlsoAppliesToActors() {
        Movie a = movie(Director.of("Dir"), List.of(Genre.of("Drama")),
                List.of(new Actor(1247, "송강호")), List.of(Country.of("South Korea")),
                "2019-05-30", List.of());
        Movie b = movie(Director.of("Dir"), List.of(Genre.of("Drama")),
                List.of(new Actor(1247, "Song Kang-ho")), List.of(Country.of("South Korea")),
                "2003-04-25", List.of());

        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(a, 9.0), new RatedMovie(b, 9.0)));

        assertThat(profile.preferredActors()).containsExactly("Song Kang-ho");
        assertThat(profile.actorAffinityOf(new Actor(1247, "송강호"))).isEqualTo(1.0);
        assertThat(profile.actorIdToName()).containsEntry(1247, "Song Kang-ho");
    }

    @Test
    void attachesNarrativePatternsDetectedFromTheHistory() {
        List<RatedMovie> history = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            history.add(new RatedMovie(new Movie(i, "tt" + i, "KT " + i, "KT " + i, "2016-01-01",
                    120, "o", List.of(Genre.of("Suspense"), Genre.of("Crimen")),
                    Director.of("Bong Joon-ho"), List.of(Actor.of("Song Kang-ho")),
                    List.of(Country.of("Corea del Sur")), null, List.of()), 9.0));
        }
        // non-matching, non-favourite films so the Korean thrillers are over-represented
        for (int i = 0; i < 40; i++) {
            history.add(new RatedMovie(new Movie(100 + i, "tt1" + i, "F " + i, "F " + i, "2016-01-01",
                    120, "o", List.of(Genre.of("Aventura")), Director.of("F " + i),
                    List.of(Actor.of("A")), List.of(Country.of("USA")), null, List.of()), 5.0));
        }

        UserTasteProfile profile = builder.build(history);

        assertThat(profile.patterns()).isNotEmpty();
        assertThat(profile.patterns()).anyMatch(p -> p.id().equals("KOREAN_THRILLER"));
    }

    @Test
    void aSmallHistoryHasNoPatterns() {
        UserTasteProfile profile = builder.build(List.of(new RatedMovie(
                fullMovie("Bong Joon-ho", "Suspense", "Song Kang-ho", "Corea del Sur", "2019-05-30"),
                9.0)));

        assertThat(profile.patterns()).isEmpty();
    }

    @Test
    void noMovieReachingTheThresholdYieldsAnEmptyProfile() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(fullMovie("Nolan", "Sci-Fi", "McConaughey", "USA", "2014-11-07"), 6.9),
                new RatedMovie(fullMovie("Bay", "Action", "LaBeouf", "USA", "2011-06-28"), 3.0)));

        assertThat(profile).isEqualTo(UserTasteProfile.empty());
    }

    @Test
    void aSingleFavouriteExtractsAllFiveDimensions() {
        UserTasteProfile profile = builder.build(List.of(new RatedMovie(
                movie(Director.of("Coralie Fargeat"),
                        List.of(Genre.of("Terror"), Genre.of("Drama")),
                        List.of(Actor.of("Demi Moore"), Actor.of("Margaret Qualley")),
                        List.of(Country.of("Francia"), Country.of("Reino Unido")),
                        "2024-09-07", List.of()),
                8.5)));

        assertThat(profile.preferredDirectors()).containsExactly("Coralie Fargeat");
        assertThat(profile.preferredGenres()).containsExactlyInAnyOrder("Terror", "Drama");
        assertThat(profile.preferredActors()).containsExactlyInAnyOrder("Demi Moore", "Margaret Qualley");
        assertThat(profile.preferredCountries()).containsExactlyInAnyOrder("Francia", "Reino Unido");
        assertThat(profile.preferredDecades()).containsExactly(2020);
    }

    @Test
    void severalFavouritesAreUnionedWithoutDuplicates() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(fullMovie("Denis Villeneuve", "Sci-Fi", "Timothee Chalamet",
                        "Canada", "2021-09-15"), 8.0),
                new RatedMovie(fullMovie("Denis Villeneuve", "Drama", "Amy Adams",
                        "Canada", "2016-11-11"), 9.0)));

        assertThat(profile.preferredDirectors()).containsExactly("Denis Villeneuve");
        assertThat(profile.preferredGenres()).containsExactlyInAnyOrder("Sci-Fi", "Drama");
        assertThat(profile.preferredActors())
                .containsExactlyInAnyOrder("Timothee Chalamet", "Amy Adams");
        assertThat(profile.preferredCountries()).containsExactly("Canada");
        assertThat(profile.preferredDecades()).containsExactlyInAnyOrder(2020, 2010);
    }

    @Test
    void aScoreExactlyAtSevenCounts() {
        UserTasteProfile profile = builder.build(List.of(new RatedMovie(
                fullMovie("Bong Joon-ho", "Thriller", "Song Kang-ho", "Corea del Sur", "2019-05-30"),
                7.0)));

        assertThat(profile.preferredDirectors()).containsExactly("Bong Joon-ho");
        assertThat(profile.preferredGenres()).containsExactly("Thriller");
        assertThat(profile.preferredDecades()).containsExactly(2010);
    }

    @Test
    void aScoreBelowSevenDoesNotCount() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(fullMovie("Good Director", "Good Genre", "Good Actor",
                        "Good Country", "2020-01-01"), 8.0),
                new RatedMovie(fullMovie("Bad Director", "Bad Genre", "Bad Actor",
                        "Bad Country", "1999-01-01"), 6.99)));

        assertThat(profile.preferredDirectors()).containsExactly("Good Director");
        assertThat(profile.preferredGenres()).containsExactly("Good Genre");
        assertThat(profile.preferredActors()).containsExactly("Good Actor");
        assertThat(profile.preferredCountries()).containsExactly("Good Country");
        assertThat(profile.preferredDecades()).containsExactly(2020);
    }

    @Test
    void duplicateEntriesDoNotProduceDuplicates() {
        RatedMovie favourite = new RatedMovie(
                fullMovie("Nolan", "Sci-Fi", "McConaughey", "USA", "2014-11-07"), 9.0);

        UserTasteProfile once = builder.build(List.of(favourite));
        UserTasteProfile twice = builder.build(List.of(favourite, favourite));

        assertThat(twice).isEqualTo(once);
        assertThat(twice.preferredDirectors()).containsExactly("Nolan");
        assertThat(twice.preferredDecades()).containsExactly(2010);
    }

    @Test
    void missingDataIsIgnored() {
        UserTasteProfile profile = builder.build(List.of(new RatedMovie(
                movie(null, List.of(), List.of(), List.of(), "N/A", List.of()),
                8.0)));

        assertThat(profile).isEqualTo(UserTasteProfile.empty());
    }

    @Test
    void theResultIsImmutable() {
        UserTasteProfile profile = builder.build(List.of(new RatedMovie(
                fullMovie("Nolan", "Sci-Fi", "McConaughey", "USA", "2014-11-07"), 9.0)));

        assertThatThrownBy(() -> profile.preferredGenres().add("Comedy"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> profile.preferredDecades().add(1990))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void doesNotModifyTheMoviesOrTheirRatings() {
        Rating tmdb = new Rating("TMDB", 7.13, 6419);
        List<Genre> genres = List.of(Genre.of("Terror"));
        Movie theMovie = movie(Director.of("Coralie Fargeat"), genres, List.of(), List.of(),
                "2024-09-07", List.of(tmdb));
        Movie snapshot = movie(Director.of("Coralie Fargeat"), genres, List.of(), List.of(),
                "2024-09-07", List.of(tmdb));

        builder.build(List.of(new RatedMovie(theMovie, 8.0)));

        assertThat(theMovie).isEqualTo(snapshot);
        assertThat(theMovie.ratings()).containsExactly(tmdb);
        assertThat(theMovie.ratings().get(0)).isSameAs(tmdb);
        assertThat(theMovie.genres()).containsExactly(Genre.of("Terror"));
    }

    @Test
    void stillDerivesTheDecadeFromTheReleaseDate() {
        Movie theMovie = fullMovie("Fav", "Terror", "Actor", "Francia", "2024-09-07");

        UserTasteProfile profile = builder.build(List.of(new RatedMovie(theMovie, 8.0)));

        // the profile keeps country/decade (unchanged); only the PERSONAL MATCH SCORE dropped them
        assertThat(profile.preferredDecades()).containsExactly(2020);
        assertThat(profile.decadeAffinityOf(2020)).isEqualTo(1.0);
    }

    @Test
    void blankAndDuplicateNamesCollapse() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(movie(Director.of("  Nolan  "),
                        List.of(Genre.of("Sci-Fi"), Genre.of("Sci-Fi")),
                        List.of(), List.of(), "2014-11-07", List.of()), 9.0),
                new RatedMovie(fullMovie("Nolan", "Sci-Fi", "McConaughey", "USA", "2010-07-16"), 8.0)));

        assertThat(profile.preferredDirectors()).containsExactly("Nolan");
        assertThat(profile.preferredGenres()).containsExactly("Sci-Fi");
        assertThat(profile.preferredDecades()).containsExactly(2010);
    }

    @Test
    void rejectsANullList() {
        assertThatThrownBy(() -> builder.build(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void ratedMovieValidatesItsInputs() {
        Movie anyMovie = fullMovie("d", "g", "a", "c", "2020-01-01");
        assertThatThrownBy(() -> new RatedMovie(null, 8.0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RatedMovie(anyMovie, 10.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RatedMovie(anyMovie, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void profileFromMixedListReflectsOnlyTheFavourites() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(fullMovie("Loved", "Drama", "A", "Spain", "2022-01-01"), 8.0),
                new RatedMovie(fullMovie("Meh", "Comedy", "B", "France", "2018-01-01"), 5.5),
                new RatedMovie(fullMovie("Loved Too", "Thriller", "C", "Spain", "2001-01-01"), 7.5)));

        assertThat(profile.preferredDirectors()).containsExactlyInAnyOrder("Loved", "Loved Too");
        assertThat(profile.preferredGenres()).containsExactlyInAnyOrder("Drama", "Thriller");
        assertThat(profile.preferredCountries()).containsExactly("Spain");
        assertThat(profile.preferredDecades()).containsExactlyInAnyOrder(2020, 2000);
        assertThat(profile.preferredGenres()).doesNotContain("Comedy");
        assertThat(profile.preferredActors()).doesNotContain("B");
    }

    // --- weighting ---------------------------------------------------------------

    @Test
    void higherRatedFavouritesWeighMoreThanThoseBarelyOverTheThreshold() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(fullMovie("Barely", "g1", "a1", "c1", "2020-01-01"), 7.0),   // weight 1.0
                new RatedMovie(fullMovie("Loved", "g2", "a2", "c2", "2020-01-01"), 10.0))); // weight 4.0

        assertThat(profile.directorAffinityOf("Loved")).isEqualTo(1.0);
        assertThat(profile.directorAffinityOf("Barely")).isEqualTo(0.25); // 1.0 / 4.0
        assertThat(profile.directorAffinityOf("Loved"))
                .isGreaterThan(profile.directorAffinityOf("Barely"));
    }

    @Test
    void moreFrequentPreferencesWeighMore() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(fullMovie("d1", "Recurring", "a1", "c1", "2020-01-01"), 7.0),
                new RatedMovie(fullMovie("d2", "Recurring", "a2", "c2", "2020-01-01"), 7.0),
                new RatedMovie(fullMovie("d3", "Recurring", "a3", "c3", "2020-01-01"), 7.0),
                new RatedMovie(fullMovie("d4", "OneOff", "a4", "c4", "2020-01-01"), 7.0)));

        assertThat(profile.genreAffinityOf("Recurring")).isEqualTo(1.0);           // 3.0 / 3.0
        assertThat(profile.genreAffinityOf("OneOff")).isCloseTo(1.0 / 3.0, within(1e-6)); // 1.0 / 3.0
    }

    @Test
    void everyPopulatedDimensionHasAStrongestValueOfExactlyOne() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(movie(Director.of("A"), List.of(Genre.of("G1"), Genre.of("G2")),
                        List.of(Actor.of("X")), List.of(Country.of("ES")), "2021-01-01", List.of()), 9.0),
                new RatedMovie(movie(Director.of("B"), List.of(Genre.of("G1")),
                        List.of(Actor.of("Y")), List.of(Country.of("FR")), "1999-01-01", List.of()), 7.5)));

        assertThat(profile.directorAffinity().values()).contains(1.0);
        assertThat(profile.genreAffinity().values()).contains(1.0);
        assertThat(profile.actorAffinity().values()).contains(1.0);
        assertThat(profile.countryAffinity().values()).contains(1.0);
        assertThat(profile.decadeAffinity().values()).contains(1.0);
        assertThat(profile.directorAffinity().values()).allSatisfy(v -> assertThat(v).isBetween(0.0, 1.0));
    }

    @Test
    void aValueThatAppearsInOnlyOneBarelyFavouriteIsStillKept() {
        UserTasteProfile profile = builder.build(List.of(
                new RatedMovie(fullMovie("Recurring", "g", "a", "c", "2020-01-01"), 10.0),
                new RatedMovie(fullMovie("Recurring", "g", "a", "c", "2020-01-01"), 10.0),
                new RatedMovie(fullMovie("Rare", "g2", "a2", "c2", "2020-01-01"), 7.0)));

        assertThat(profile.preferredDirectors()).contains("Rare");
        assertThat(profile.directorAffinityOf("Rare")).isGreaterThan(0.0);
    }

    @Test
    void theWeightingIsDeterministicAndReproducible() {
        List<RatedMovie> input = List.of(
                new RatedMovie(fullMovie("A", "G1", "X", "ES", "2020-01-01"), 8.0),
                new RatedMovie(fullMovie("B", "G2", "Y", "FR", "2011-01-01"), 9.5),
                new RatedMovie(fullMovie("A", "G1", "Z", "ES", "2015-01-01"), 7.0));

        assertThat(builder.build(input)).isEqualTo(builder.build(input));
    }
}
