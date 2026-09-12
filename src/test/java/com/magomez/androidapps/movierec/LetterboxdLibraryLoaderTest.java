package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.provider.MovieDataProvider;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.scoring.UserTasteProfileBuilder;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdCsvParser;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdImportService;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibrary;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibraryLoader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The new part this class adds over {@code LetterboxdRatingsResourceLoader}: it collects,
 * in the same pass, the TMDB ids of every identified Letterboxd movie so candidates that
 * are already seen can be excluded by id.
 */
class LetterboxdLibraryLoaderTest {

    private final FakeProvider provider = new FakeProvider();

    private LetterboxdLibraryLoader loaderFor(String resourcePath) {
        return new LetterboxdLibraryLoader(
                new LetterboxdImportService(provider, new LetterboxdCsvParser()),
                new UserTasteProfileBuilder(),
                resourcePath);
    }

    private static Movie movie(int tmdbId, String director, String genre, String actor,
                               String country, String releaseDate) {
        return new Movie(tmdbId, "tt" + tmdbId, "T" + tmdbId, "T" + tmdbId, releaseDate, 120, "o",
                List.of(Genre.of(genre)), Director.of(director), List.of(Actor.of(actor)),
                List.of(Country.of(country)), null, List.of());
    }

    @Test
    void watchedSetHoldsEveryIdentifiedMovieEvenTheOnesBelowTheFavouriteThreshold() throws Exception {
        provider.identifyAs("Dune",
                movie(1, "Denis Villeneuve", "Sci-Fi", "Timothee Chalamet", "Canada", "2021-09-15"));
        provider.identifyAs("Parasite",
                movie(2, "Bong Joon-ho", "Thriller", "Song Kang-ho", "South Korea", "2019-05-30"));
        // rated 2/5 -> userScore 4.0 -> not a favourite, but still an identified/seen movie
        provider.identifyAs("Meh Movie",
                movie(3, "Meh Director", "Comedy", "Meh Actor", "Nowhere", "2010-01-01"));

        LetterboxdLibrary library = loaderFor("/letterboxd/ratings-test.csv").load();

        assertThat(library.watchedTmdbIds()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(library.hasWatched(2)).isTrue();
        assertThat(library.hasWatched(999)).isFalse();
        // the watched movie keeps its title and the user's rating, for naming similar films
        assertThat(library.watchedMovie(2)).hasValueSatisfying(w -> {
            assertThat(w.title()).isEqualTo("T2");
            assertThat(w.userScore()).isEqualTo(10.0); // Parasite rated 5/5
        });
        // the profile still follows the userScore >= 7.0 rule (unchanged)
        assertThat(library.profile().preferredDirectors())
                .containsExactlyInAnyOrder("Denis Villeneuve", "Bong Joon-ho");
    }

    @Test
    void notFoundEntriesContributeNoWatchedId() throws Exception {
        provider.identifyAs("Dune",
                movie(1, "Denis Villeneuve", "Sci-Fi", "Timothee Chalamet", "Canada", "2021-09-15"));
        // "Parasite" and "Meh Movie" are left unknown -> NOT_FOUND

        LetterboxdLibrary library = loaderFor("/letterboxd/ratings-test.csv").load();

        assertThat(library.watchedTmdbIds()).containsExactly(1);
    }

    @Test
    void theLibraryIsBuiltOnceAndReusedOnEveryFurtherLoad() throws Exception {
        provider.identifyAs("Dune",
                movie(1, "Denis Villeneuve", "Sci-Fi", "Timothee Chalamet", "Canada", "2021-09-15"));
        LetterboxdLibraryLoader loader = loaderFor("/letterboxd/ratings-test.csv");

        LetterboxdLibrary first = loader.load();
        int callsAfterFirst = provider.identifyCalls;
        LetterboxdLibrary second = loader.load();

        assertThat(second).isSameAs(first);
        assertThat(provider.identifyCalls).isEqualTo(callsAfterFirst); // no re-identification
    }

    @Test
    void aMissingResourceIsAClearError() {
        assertThatThrownBy(() -> loaderFor("/letterboxd/does-not-exist.csv").load())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does-not-exist.csv");
    }

    /** Hand-written {@link MovieDataProvider}: no real TMDB call. */
    private static final class FakeProvider implements MovieDataProvider {

        private final Map<String, MovieMatch> answers = new HashMap<>();
        private volatile int identifyCalls;

        void identifyAs(String title, Movie movie) {
            answers.put(title, MovieMatch.identified(movie));
        }

        @Override
        public String sourceName() {
            return "FAKE";
        }

        @Override
        public synchronized MovieMatch identify(MovieQuery query) throws MovieProviderException {
            identifyCalls++;
            return answers.getOrDefault(query.title(), MovieMatch.notFound());
        }
    }
}
