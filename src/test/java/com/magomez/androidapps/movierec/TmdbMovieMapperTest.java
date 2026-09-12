package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.tmdb.TmdbMovieMapper;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails.TmdbCast;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails.TmdbCredits;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails.TmdbCrew;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails.TmdbGenre;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails.TmdbProductionCountry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TmdbMovieMapperTest {

    @Test
    void mapsAllRelevantFieldsFromTmdbDetails() {
        TmdbMovieDetails details = new TmdbMovieDetails(
                603,
                "tt0133093",
                "Matrix",
                "The Matrix",
                "1999-03-30",
                136,
                "Un hacker descubre la verdad.",
                "/poster.jpg",
                List.of(new TmdbGenre(28, "Acción"), new TmdbGenre(878, "Ciencia ficción")),
                List.of(new TmdbProductionCountry("US", "Estados Unidos de América")),
                new TmdbCredits(
                        List.of(
                                new TmdbCast(6384, "Keanu Reeves", 0),
                                new TmdbCast(2975, "Laurence Fishburne", 1),
                                new TmdbCast(1331, "Carrie-Anne Moss", 2)),
                        List.of(
                                new TmdbCrew(7, "Someone Else", "Producer"),
                                new TmdbCrew(9339, "Lana Wachowski", "Director"),
                                new TmdbCrew(9340, "Lilly Wachowski", "Director"))),
                8.2,
                26000);

        Movie movie = TmdbMovieMapper.toMovie(details);

        assertThat(movie.tmdbId()).isEqualTo(603);
        assertThat(movie.imdbId()).isEqualTo("tt0133093");
        assertThat(movie.title()).isEqualTo("Matrix");
        assertThat(movie.originalTitle()).isEqualTo("The Matrix");
        assertThat(movie.releaseDate()).isEqualTo("1999-03-30");
        assertThat(movie.runtime()).isEqualTo(136);
        assertThat(movie.overview()).isEqualTo("Un hacker descubre la verdad.");
        assertThat(movie.poster()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");

        assertThat(movie.genres())
                .containsExactly(new Genre(28, "Acción"), new Genre(878, "Ciencia ficción"));
        assertThat(movie.countries())
                .containsExactly(new Country("US", "Estados Unidos de América"));
        assertThat(movie.director()).isEqualTo(new Director(9339, "Lana Wachowski"));
        assertThat(movie.actors()).containsExactly(
                new Actor(6384, "Keanu Reeves"),
                new Actor(2975, "Laurence Fishburne"),
                new Actor(1331, "Carrie-Anne Moss"));
        // ratings are not fetched by toMovie(); they are enriched separately
        assertThat(movie.ratings()).isEmpty();
    }

    @Test
    void ordersActorsByCreditOrderAndCapsAtTen() {
        List<TmdbCast> cast = List.of(
                new TmdbCast(11, "A11", 10),
                new TmdbCast(1, "A1", 0),
                new TmdbCast(3, "A3", 2),
                new TmdbCast(2, "A2", 1),
                new TmdbCast(5, "A5", 4),
                new TmdbCast(4, "A4", 3),
                new TmdbCast(7, "A7", 6),
                new TmdbCast(6, "A6", 5),
                new TmdbCast(9, "A9", 8),
                new TmdbCast(8, "A8", 7),
                new TmdbCast(10, "A10", 9));

        TmdbMovieDetails details = new TmdbMovieDetails(1, null, "t", "t", null, null, null, null,
                null, null, new TmdbCredits(cast, List.of()), null, null);

        Movie movie = TmdbMovieMapper.toMovie(details);

        assertThat(movie.actors()).hasSize(10);
        assertThat(movie.actors()).extracting(Actor::name)
                .containsExactly(IntStream.rangeClosed(1, 10)
                        .mapToObj(i -> "A" + i).toArray(String[]::new));
    }

    @Test
    void toleratesMissingOptionalData() {
        TmdbMovieDetails details = new TmdbMovieDetails(1, "", "t", "t", null, null, null, null,
                null, null, null, null, null);

        Movie movie = TmdbMovieMapper.toMovie(details);

        assertThat(movie.imdbId()).isNull();
        assertThat(movie.director()).isNull();
        assertThat(movie.poster()).isNull();
        assertThat(movie.genres()).isEmpty();
        assertThat(movie.actors()).isEmpty();
        assertThat(movie.countries()).isEmpty();
        assertThat(movie.similarMovies()).isEmpty();
    }

    @Test
    void mapsTmdbSimilarMoviesWithIdTitleAndYearCappedAtTwenty() {
        List<TmdbMovieDetails.TmdbSimilarResult> similar = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> new TmdbMovieDetails.TmdbSimilarResult(i, "Similar " + i, "20" + (10 + i % 10) + "-01-01"))
                .toList();
        TmdbMovieDetails details = new TmdbMovieDetails(1, "tt1", "t", "t", null, null, null, null,
                null, null, null, new TmdbMovieDetails.TmdbSimilar(similar), null, null);

        Movie movie = TmdbMovieMapper.toMovie(details);

        assertThat(movie.similarMovies()).hasSize(20);
        assertThat(movie.similarMovies().get(0).tmdbId()).isEqualTo(1);
        assertThat(movie.similarMovies().get(0).title()).isEqualTo("Similar 1");
        assertThat(movie.similarMovies().get(0).year()).isEqualTo(2011);
    }

    @Test
    void dropsSimilarEntriesWithoutAnId() {
        TmdbMovieDetails.TmdbSimilar similar = new TmdbMovieDetails.TmdbSimilar(List.of(
                new TmdbMovieDetails.TmdbSimilarResult(null, "No id", "2020-01-01"),
                new TmdbMovieDetails.TmdbSimilarResult(42, "Kept", null)));
        TmdbMovieDetails details = new TmdbMovieDetails(1, "tt1", "t", "t", null, null, null, null,
                null, null, null, similar, null, null);

        Movie movie = TmdbMovieMapper.toMovie(details);

        assertThat(movie.similarMovies()).extracting(m -> m.tmdbId()).containsExactly(42);
        assertThat(movie.similarMovies().get(0).year()).isNull();
    }

    @Test
    void keepsOnlySimilarMoviesThatShareAGenreWithThisFilm() {
        TmdbMovieDetails.TmdbSimilar similar = new TmdbMovieDetails.TmdbSimilar(List.of(
                new TmdbMovieDetails.TmdbSimilarResult(10, "Same genre", "2019-01-01", List.of(27, 53)),
                new TmdbMovieDetails.TmdbSimilarResult(11, "Unrelated", "2019-01-01", List.of(35, 10751)),
                new TmdbMovieDetails.TmdbSimilarResult(12, "No genre_ids", "2019-01-01", List.of())));
        TmdbMovieDetails details = new TmdbMovieDetails(1, "tt1", "t", "t", null, null, null, null,
                List.of(new TmdbGenre(27, "Terror"), new TmdbGenre(9648, "Misterio")),
                null, null, similar, null, null);

        Movie movie = TmdbMovieMapper.toMovie(details);

        assertThat(movie.similarMovies()).extracting(m -> m.tmdbId()).containsExactly(10);
    }

    // --- toRating ----------------------------------------------------------------

    private static TmdbMovieDetails detailsWithVotes(Double voteAverage, Integer voteCount) {
        return new TmdbMovieDetails(1, "tt1", "t", "t", null, null, null, null,
                null, null, null, voteAverage, voteCount);
    }

    @Test
    void mapsVoteAverageAndVoteCountToTmdbRating() {
        assertThat(TmdbMovieMapper.toRating(detailsWithVotes(8.1, 1_250_000)))
                .contains(new Rating("TMDB", 8.1, 1_250_000));
    }

    @Test
    void mapsRatingWithUnknownVoteCountWhenTmdbOmitsIt() {
        assertThat(TmdbMovieMapper.toRating(detailsWithVotes(7.4, null)))
                .contains(new Rating("TMDB", 7.4, null));
    }

    @Test
    void hasNoRatingWhenVoteAverageIsAbsent() {
        assertThat(TmdbMovieMapper.toRating(detailsWithVotes(null, 100))).isEmpty();
    }

    @Test
    void hasNoRatingWhenThereAreZeroVotes() {
        assertThat(TmdbMovieMapper.toRating(detailsWithVotes(0.0, 0))).isEmpty();
    }
}
