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
import com.magomez.androidapps.movierec.scoring.RatedMovie;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.UserTasteProfileBuilder;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdCsvParser;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdImportResult;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdImportService;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdRatedMovie;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdTasteProfileService;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LetterboxdTasteProfileServiceTest {

    private final FakeProvider provider = new FakeProvider();
    private final LetterboxdTasteProfileService service = new LetterboxdTasteProfileService(
            new LetterboxdImportService(provider, new LetterboxdCsvParser()),
            new UserTasteProfileBuilder());

    private static final String HEADER = "Date,Name,Year,Letterboxd URI,Rating\n";

    private UserTasteProfile buildProfile(String csv) throws Exception {
        return service.buildProfile(new StringReader(csv));
    }

    private static Movie movie(int tmdbId, String director, String genre, String actor,
                               String country, String releaseDate) {
        return new Movie(tmdbId, "tt" + tmdbId, "T" + tmdbId, "T" + tmdbId, releaseDate, 120, "o",
                List.of(Genre.of(genre)), Director.of(director), List.of(Actor.of(actor)),
                List.of(Country.of(country)), null, List.of());
    }

    @Test
    void aCsvWithSeveralIdentifiedMoviesProducesTheExpectedProfile() throws Exception {
        provider.identifyAs("Dune", movie(1, "Denis Villeneuve", "Sci-Fi", "Timothee Chalamet",
                "Canada", "2021-09-15"));
        provider.identifyAs("Parasite", movie(2, "Bong Joon-ho", "Thriller", "Song Kang-ho",
                "South Korea", "2019-05-30"));

        UserTasteProfile profile = buildProfile(HEADER
                + "d,Dune,2021,u1,4\n"
                + "d,Parasite,2019,u2,5\n");

        assertThat(profile.preferredDirectors())
                .containsExactlyInAnyOrder("Denis Villeneuve", "Bong Joon-ho");
        assertThat(profile.preferredGenres()).containsExactlyInAnyOrder("Sci-Fi", "Thriller");
        assertThat(profile.preferredActors())
                .containsExactlyInAnyOrder("Timothee Chalamet", "Song Kang-ho");
        assertThat(profile.preferredCountries())
                .containsExactlyInAnyOrder("Canada", "South Korea");
        assertThat(profile.preferredDecades()).containsExactlyInAnyOrder(2020, 2010);
    }

    @Test
    void notFoundAndAmbiguousEntriesDoNotContributeToTheProfile() throws Exception {
        provider.identifyAs("Loved", movie(1, "Fav Director", "Drama", "Fav Actor",
                "Spain", "2022-01-01"));
        provider.answer("Crash", MovieMatch.ambiguous(List.of("Crash (1996)", "Crash (2004)")));
        // "Ghost movie" is unknown -> NOT_FOUND by default

        UserTasteProfile profile = buildProfile(HEADER
                + "d,Loved,2022,u1,5\n"
                + "d,Crash,,u2,5\n"
                + "d,Ghost movie,1999,u3,5\n");

        assertThat(profile.preferredDirectors()).containsExactly("Fav Director");
        assertThat(profile.preferredGenres()).containsExactly("Drama");
        assertThat(profile.preferredActors()).containsExactly("Fav Actor");
        assertThat(profile.preferredCountries()).containsExactly("Spain");
        assertThat(profile.preferredDecades()).containsExactly(2020);
    }

    @Test
    void ratingsBelowSevenDoNotProducePreferences() throws Exception {
        provider.identifyAs("Loved", movie(1, "Loved Director", "Drama", "Loved Actor",
                "Spain", "2022-01-01"));
        provider.identifyAs("Meh", movie(2, "Meh Director", "Comedy", "Meh Actor",
                "France", "1999-01-01"));

        // "Meh" is IDENTIFIED but rated 3.0 -> userScore 6.0 -> the builder drops it
        UserTasteProfile profile = buildProfile(HEADER
                + "d,Loved,2022,u1,4\n"
                + "d,Meh,1999,u2,3\n");

        assertThat(profile.preferredDirectors()).containsExactly("Loved Director");
        assertThat(profile.preferredGenres()).containsExactly("Drama");
        assertThat(profile.preferredGenres()).doesNotContain("Comedy");
        assertThat(profile.preferredDecades()).containsExactly(2020);
    }

    @Test
    void theProfileDimensionsComeFromTheIdentifiedMoviesNotFromTheCsv() throws Exception {
        // CSV has no director column and year 2021; the identified Movie has richer data
        provider.identifyAs("Dune", movie(7, "Denis Villeneuve", "Sci-Fi", "Timothee Chalamet",
                "Canada", "1984-12-14"));

        UserTasteProfile profile = buildProfile(HEADER + "d,Dune,2021,u1,4\n");

        assertThat(profile.preferredDirectors()).containsExactly("Denis Villeneuve");
        assertThat(profile.preferredGenres()).containsExactly("Sci-Fi");
        assertThat(profile.preferredActors()).containsExactly("Timothee Chalamet");
        assertThat(profile.preferredCountries()).containsExactly("Canada");
        // decade comes from the Movie's releaseDate (1984), not from the CSV year (2021)
        assertThat(profile.preferredDecades()).containsExactly(1980);
    }

    @Test
    void anEmptyCsvBodyYieldsAnEmptyProfile() throws Exception {
        assertThat(buildProfile(HEADER)).isEqualTo(UserTasteProfile.empty());
        assertThat(provider.received).isEmpty();
    }

    @Test
    void identificationFailuresDoNotContributeAndDoNotStopTheImport() throws Exception {
        provider.identifyAs("Fine", movie(1, "D", "G", "A", "C", "2020-01-01"));
        provider.failFor("Boom", new MovieProviderException("TMDB down"));

        UserTasteProfile profile = buildProfile(HEADER
                + "d,Boom,2020,u1,5\n"
                + "d,Fine,2020,u2,5\n");

        assertThat(profile.preferredDirectors()).containsExactly("D");
    }

    @Test
    void reusesBothServicesInOrderWithoutDuplicatingLogic() throws Exception {
        RatedMovie ratedA = new RatedMovie(movie(1, "DA", "GA", "AA", "CA", "2020-01-01"), 8.0);
        RatedMovie ratedD = new RatedMovie(movie(4, "DD", "GD", "AD", "CD", "2010-01-01"), 9.0);
        LetterboxdRatedMovie entryA = new LetterboxdRatedMovie("A", 2020, "ua", 8.0);
        LetterboxdRatedMovie entryB = new LetterboxdRatedMovie("B", 2019, "ub", 8.0);
        LetterboxdRatedMovie entryC = new LetterboxdRatedMovie("C", 2004, "uc", 8.0);
        LetterboxdRatedMovie entryD = new LetterboxdRatedMovie("D", 2010, "ud", 9.0);

        AtomicReference<Reader> readerSeenByImport = new AtomicReference<>();
        List<RatedMovie> ratedMoviesSeenByBuilder = new ArrayList<>();
        UserTasteProfile sentinel =
                new UserTasteProfile(Set.of("SENTINEL"), Set.of(), Set.of(), Set.of(), Set.of());

        LetterboxdImportService importStub =
                new LetterboxdImportService(provider, new LetterboxdCsvParser()) {
                    @Override
                    public List<LetterboxdImportResult> importRatings(Reader reader) {
                        readerSeenByImport.set(reader);
                        return List.of(
                                LetterboxdImportResult.identified(entryA, ratedA),
                                LetterboxdImportResult.notFound(entryB),
                                LetterboxdImportResult.ambiguous(entryC, List.of("C (2004)", "C (2005)")),
                                LetterboxdImportResult.identified(entryD, ratedD));
                    }
                };
        UserTasteProfileBuilder builderStub = new UserTasteProfileBuilder() {
            @Override
            public UserTasteProfile build(List<RatedMovie> ratedMovies) {
                ratedMoviesSeenByBuilder.addAll(ratedMovies);
                return sentinel;
            }
        };

        Reader reader = new StringReader("ignored by the stub");
        UserTasteProfile result =
                new LetterboxdTasteProfileService(importStub, builderStub).buildProfile(reader);

        assertThat(readerSeenByImport.get()).isSameAs(reader);      // reader passed straight through
        assertThat(ratedMoviesSeenByBuilder).containsExactly(ratedA, ratedD); // only IDENTIFIED, in order
        assertThat(result).isSameAs(sentinel);                      // returns exactly the builder's output
    }

    @Test
    void rejectsANullReader() {
        assertThatThrownBy(() -> service.buildProfile(null))
                .isInstanceOf(NullPointerException.class);
    }

    /** Hand-written {@link MovieDataProvider}: no real TMDB call. */
    private static final class FakeProvider implements MovieDataProvider {

        final List<MovieQuery> received = new ArrayList<>();
        private final Map<String, MovieMatch> answers = new HashMap<>();
        private final Map<String, MovieProviderException> failures = new HashMap<>();

        void identifyAs(String title, Movie movie) {
            answers.put(title, MovieMatch.identified(movie));
        }

        void answer(String title, MovieMatch match) {
            answers.put(title, match);
        }

        void failFor(String title, MovieProviderException error) {
            failures.put(title, error);
        }

        @Override
        public String sourceName() {
            return "FAKE";
        }

        @Override
        public MovieMatch identify(MovieQuery query) throws MovieProviderException {
            received.add(query);
            MovieProviderException failure = failures.get(query.title());
            if (failure != null) {
                throw failure;
            }
            return answers.getOrDefault(query.title(), MovieMatch.notFound());
        }
    }
}
