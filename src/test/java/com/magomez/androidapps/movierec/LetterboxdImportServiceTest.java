package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.provider.MovieDataProvider;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.scoring.RatedMovie;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdCsvException;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdCsvParser;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdImportResult;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdImportService;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdRatedMovie;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LetterboxdImportServiceTest {

    private final FakeProvider provider = new FakeProvider();
    private final LetterboxdImportService service =
            new LetterboxdImportService(provider, new LetterboxdCsvParser());

    private static final String HEADER = "Date,Name,Year,Letterboxd URI,Rating\n";

    private List<LetterboxdImportResult> importCsv(String csv) throws Exception {
        return service.importRatings(new StringReader(csv));
    }

    private static Movie movie(String title, int tmdbId) {
        return new Movie(tmdbId, "tt" + tmdbId, title, title, null, null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
    }

    @Test
    void keepsCsvOrderAcrossSeveralEntries() throws Exception {
        provider.identifyAs("First", movie("First", 1));
        provider.identifyAs("Second", movie("Second", 2));
        provider.identifyAs("Third", movie("Third", 3));

        List<LetterboxdImportResult> results = importCsv(HEADER
                + "d,First,2020,u1,4\n"
                + "d,Second,2021,u2,3\n"
                + "d,Third,2022,u3,5\n");

        assertThat(results).extracting(LetterboxdImportResult::letterboxdTitle)
                .containsExactly("First", "Second", "Third");
        assertThat(results).extracting(r -> r.ratedMovie().movie().tmdbId())
                .containsExactly(1, 2, 3);
    }

    @Test
    void anIdentifiedEntryProducesARatedMovieWithTheUserScore() throws Exception {
        Movie identified = movie("Love Hurts", 42);
        provider.identifyAs("Love Hurts", identified);

        LetterboxdImportResult result = importCsv(HEADER
                + "2025-12-31,Love Hurts,2025,https://boxd.it/KyBc,4\n").get(0);

        assertThat(result.isIdentified()).isTrue();
        assertThat(result.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(result.ratedMovie()).isEqualTo(new RatedMovie(identified, 8.0));
        assertThat(result.ratedMovie().movie()).isSameAs(identified);
        assertThat(result.userScore()).isEqualTo(8.0);
        assertThat(result.identificationError()).isNull();
    }

    @Test
    void theYearIsUsedInTheMovieQuery() throws Exception {
        provider.identifyAs("Dune", movie("Dune", 1));

        importCsv(HEADER + "d,Dune,2021,u,4\n");

        assertThat(provider.received).singleElement()
                .isEqualTo(new MovieQuery("Dune", 2021, null));
        assertThat(provider.received.get(0).hasYear()).isTrue();
    }

    @Test
    void aNullYearProducesAQueryWithoutYear() throws Exception {
        provider.identifyAs("Untitled", movie("Untitled", 1));

        importCsv(HEADER + "d,Untitled,,u,4\n");

        assertThat(provider.received).singleElement()
                .isEqualTo(new MovieQuery("Untitled", null, null));
        assertThat(provider.received.get(0).year()).isNull();
        assertThat(provider.received.get(0).hasYear()).isFalse();
    }

    @Test
    void aNotFoundEntryIsKeptWithoutARatedMovie() throws Exception {
        // provider returns NOT_FOUND by default for unknown titles
        LetterboxdImportResult result = importCsv(HEADER + "d,Unknown movie,2000,u,4\n").get(0);

        assertThat(result.status()).isEqualTo(IdentificationStatus.NOT_FOUND);
        assertThat(result.ratedMovie()).isNull();
        assertThat(result.candidates()).isEmpty();
        assertThat(result.identificationError()).isNull();
        assertThat(result.letterboxdTitle()).isEqualTo("Unknown movie");
        assertThat(result.userScore()).isEqualTo(8.0);
    }

    @Test
    void anAmbiguousEntryKeepsTheCandidatesWithoutARatedMovie() throws Exception {
        provider.answer("Crash", MovieMatch.ambiguous(List.of("Crash (1996)", "Crash (2004)")));

        LetterboxdImportResult result = importCsv(HEADER + "d,Crash,,u,3.5\n").get(0);

        assertThat(result.status()).isEqualTo(IdentificationStatus.AMBIGUOUS);
        assertThat(result.ratedMovie()).isNull();
        assertThat(result.candidates()).containsExactly("Crash (1996)", "Crash (2004)");
    }

    @Test
    void oneProblematicEntryDoesNotStopTheFollowingOnes() throws Exception {
        provider.identifyAs("Good One", movie("Good One", 1));
        provider.failFor("Broken", new MovieProviderException("TMDB 503"));
        provider.identifyAs("Good Two", movie("Good Two", 2));

        List<LetterboxdImportResult> results = importCsv(HEADER
                + "d,Good One,2019,u1,4\n"
                + "d,Broken,2020,u2,4.5\n"
                + "d,Good Two,2021,u3,5\n");

        assertThat(results).extracting(LetterboxdImportResult::status).containsExactly(
                IdentificationStatus.IDENTIFIED,
                IdentificationStatus.NOT_FOUND,
                IdentificationStatus.IDENTIFIED);
        assertThat(results.get(1).identificationError()).isEqualTo("TMDB 503");
        assertThat(results.get(1).ratedMovie()).isNull();
        assertThat(results.get(0).ratedMovie().movie().tmdbId()).isEqualTo(1);
        assertThat(results.get(2).ratedMovie().movie().tmdbId()).isEqualTo(2);
    }

    @Test
    void aUserScoreOfSevenIsPreservedExactly() throws Exception {
        provider.identifyAs("Parasite", movie("Parasite", 1));

        LetterboxdImportResult result = importCsv(HEADER + "d,Parasite,2019,u,3.5\n").get(0);

        assertThat(result.userScore()).isEqualTo(7.0);
        assertThat(result.ratedMovie().userScore()).isEqualTo(7.0);
    }

    @Test
    void theOriginalLetterboxdTitleAndUriArePreserved() throws Exception {
        provider.identifyAs("tick, tick... BOOM!", movie("tick, tick... BOOM!", 1));

        LetterboxdImportResult result = importCsv(HEADER
                + "2026-03-08,\"tick, tick... BOOM!\",2021,https://boxd.it/jz2e,4\n").get(0);

        assertThat(result.letterboxdTitle()).isEqualTo("tick, tick... BOOM!");
        assertThat(result.letterboxdUri()).isEqualTo("https://boxd.it/jz2e");
        assertThat(result.year()).isEqualTo(2021);
    }

    @Test
    void reusesTheMovieDataProviderWithoutAnyDuplicateMatchingLogic() throws Exception {
        provider.identifyAs("A", movie("A", 1));
        provider.identifyAs("B", movie("B", 2));

        importCsv(HEADER + "d,A,2001,uri-a,4\n" + "d,B,,uri-b,3\n");

        // exactly one identify() call per CSV entry, each a plain title+year query, director null
        assertThat(provider.received).containsExactly(
                new MovieQuery("A", 2001, null),
                new MovieQuery("B", null, null));
        assertThat(provider.received).allSatisfy(q -> assertThat(q.director()).isNull());
    }

    @Test
    void aParserFailurePropagates() {
        assertThatThrownBy(() -> importCsv("Date,Name,Year,Letterboxd URI\n" + "d,Movie,2020,u\n"))
                .isInstanceOf(LetterboxdCsvException.class);
    }

    @Test
    void emptyCsvBodyProducesNoResults() throws Exception {
        assertThat(importCsv(HEADER)).isEmpty();
        assertThat(provider.received).isEmpty();
    }

    @Test
    void letterboxdImportResultEnforcesConsistency() {
        LetterboxdRatedMovie entry = new LetterboxdRatedMovie("t", 2020, "u", 8.0);
        RatedMovie ratedMovie = new RatedMovie(movie("t", 1), 8.0);

        assertThatThrownBy(() -> new LetterboxdImportResult("t", 2020, "u", 8.0,
                IdentificationStatus.IDENTIFIED, List.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LetterboxdImportResult("t", 2020, "u", 8.0,
                IdentificationStatus.NOT_FOUND, List.of(), ratedMovie, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(LetterboxdImportResult.identified(entry, ratedMovie).isIdentified()).isTrue();
    }

    /** Hand-written {@link MovieDataProvider}: no real TMDB call, records every query. */
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
