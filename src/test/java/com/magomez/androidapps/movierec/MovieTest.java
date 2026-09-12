package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieTest {

    private static Movie movie(List<Rating> ratings) {
        return new Movie(603, "tt0133093", "Matrix", "The Matrix", "1999-03-30", 136,
                "overview", List.of(), null, List.of(), List.of(), null, ratings);
    }

    @Test
    void existsWithAnEmptyRatingsCollectionWhenNoneAreAvailable() {
        assertThat(movie(null).ratings()).isEmpty();
        assertThat(movie(List.of()).ratings()).isEmpty();
    }

    @Test
    void holdsASingleRating() {
        Rating tmdb = new Rating("TMDB", 8.2, 26_000);

        assertThat(movie(List.of(tmdb)).ratings()).containsExactly(tmdb);
    }

    @Test
    void holdsSeveralRatingsFromGenericSources() {
        Rating tmdb = new Rating("TMDB", 8.2, 26_000);
        Rating imdb = new Rating("IMDb", 8.7, 2_000_000);

        assertThat(movie(List.of(tmdb, imdb)).ratings()).containsExactly(tmdb, imdb);
    }

    @Test
    void ratingsCollectionIsUnmodifiable() {
        Movie movie = movie(List.of(new Rating("TMDB", 8.2, 26_000)));

        assertThatThrownBy(() -> movie.ratings().add(new Rating("IMDb", 8.7, 10)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void ratingsAreDefensivelyCopiedFromTheConstructorArgument() {
        List<Rating> source = new ArrayList<>();
        source.add(new Rating("TMDB", 8.2, 26_000));

        Movie movie = movie(source);
        source.add(new Rating("IMDb", 8.7, 10));

        assertThat(movie.ratings()).hasSize(1);
    }

    @Test
    void withRatingsReturnsANewMovieAndLeavesTheOriginalUntouched() {
        Movie original = movie(List.of());
        Rating rating = new Rating("TMDB", 8.2, 26_000);

        Movie enriched = original.withRatings(List.of(rating));

        assertThat(original.ratings()).isEmpty();
        assertThat(enriched.ratings()).containsExactly(rating);
        assertThat(enriched).isNotSameAs(original);
        // every other field is preserved
        assertThat(enriched).usingRecursiveComparison()
                .ignoringFields("ratings")
                .isEqualTo(original);
    }

    @Test
    void withRatingsAlsoProducesAnUnmodifiableCollection() {
        Movie enriched = movie(List.of()).withRatings(new ArrayList<>(List.of(new Rating("TMDB", 8.2, 1))));

        assertThatThrownBy(() -> enriched.ratings().add(new Rating("IMDb", 8.7, 10)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
