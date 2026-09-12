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
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.UserTasteProfileBuilder;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdCsvParser;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdImportService;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdRatingsResourceLoader;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdTasteProfileService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LetterboxdRatingsResourceLoaderTest {

    private final FakeProvider provider = new FakeProvider();

    private LetterboxdRatingsResourceLoader loaderFor(String resourcePath) {
        LetterboxdTasteProfileService profileService = new LetterboxdTasteProfileService(
                new LetterboxdImportService(provider, new LetterboxdCsvParser()),
                new UserTasteProfileBuilder());
        return new LetterboxdRatingsResourceLoader(profileService, resourcePath);
    }

    private static Movie movie(int tmdbId, String director, String genre, String actor,
                               String country, String releaseDate) {
        return new Movie(tmdbId, "tt" + tmdbId, "T" + tmdbId, "T" + tmdbId, releaseDate, 120, "o",
                List.of(Genre.of(genre)), Director.of(director), List.of(Actor.of(actor)),
                List.of(Country.of(country)), null, List.of());
    }

    @Test
    void loadsTheTestCsvFromTheClasspathAndBuildsTheProfile() throws Exception {
        provider.identifyAs("Dune",
                movie(1, "Denis Villeneuve", "Sci-Fi", "Timothee Chalamet", "Canada", "2021-09-15"));
        provider.identifyAs("Parasite",
                movie(2, "Bong Joon-ho", "Thriller", "Song Kang-ho", "South Korea", "2019-05-30"));
        provider.identifyAs("Meh Movie",
                movie(3, "Meh Director", "Comedy", "Meh Actor", "Nowhere", "2010-01-01"));

        UserTasteProfile profile = loaderFor("/letterboxd/ratings-test.csv").loadProfile();

        // "Meh Movie" is identified but rated 2 -> userScore 4.0 -> not a favourite
        assertThat(profile.preferredDirectors())
                .containsExactlyInAnyOrder("Denis Villeneuve", "Bong Joon-ho");
        assertThat(profile.preferredGenres()).containsExactlyInAnyOrder("Sci-Fi", "Thriller");
        assertThat(profile.preferredActors())
                .containsExactlyInAnyOrder("Timothee Chalamet", "Song Kang-ho");
        assertThat(profile.preferredCountries())
                .containsExactlyInAnyOrder("Canada", "South Korea");
        assertThat(profile.preferredDecades()).containsExactlyInAnyOrder(2020, 2010);
        assertThat(profile.preferredGenres()).doesNotContain("Comedy");
    }

    @Test
    void aHeaderOnlyResourceYieldsAnEmptyProfileWithoutAnyIdentification() throws Exception {
        UserTasteProfile profile = loaderFor("/letterboxd/ratings-empty.csv").loadProfile();

        assertThat(profile).isEqualTo(UserTasteProfile.empty());
        assertThat(provider.received).isEmpty(); // header only -> no rows -> no identification
    }

    @Test
    void loadsTheBundledResourceFromTheClasspath() throws Exception {
        // The real /letterboxd/ratings.csv is present; with a fake provider nothing
        // identifies, so this just checks the resource is found and the pipeline runs.
        UserTasteProfile profile = loaderFor("/letterboxd/ratings.csv").loadProfile();

        assertThat(profile).isNotNull();
    }

    @Test
    void aMissingResourceIsAClearError() {
        assertThatThrownBy(() -> loaderFor("/letterboxd/does-not-exist.csv").loadProfile())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does-not-exist.csv");
    }

    /** Hand-written {@link MovieDataProvider}: no real TMDB call. */
    private static final class FakeProvider implements MovieDataProvider {

        final List<MovieQuery> received = new ArrayList<>();
        private final Map<String, MovieMatch> answers = new HashMap<>();

        void identifyAs(String title, Movie movie) {
            answers.put(title, MovieMatch.identified(movie));
        }

        @Override
        public String sourceName() {
            return "FAKE";
        }

        @Override
        public MovieMatch identify(MovieQuery query) throws MovieProviderException {
            received.add(query);
            return answers.getOrDefault(query.title(), MovieMatch.notFound());
        }
    }
}
