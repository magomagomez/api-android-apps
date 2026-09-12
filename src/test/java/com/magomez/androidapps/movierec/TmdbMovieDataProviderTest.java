package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.provider.tmdb.TmdbClient;
import com.magomez.androidapps.movierec.provider.tmdb.TmdbMovieDataProvider;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails.TmdbCredits;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails.TmdbCrew;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbSearchResponse;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbSearchResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses a hand-written {@link TmdbClient} test double instead of a mocking framework:
 * the client is a concrete class and the CI JVM cannot always instrument it. No test
 * hits the real TMDB API.
 */
class TmdbMovieDataProviderTest {

    private final FakeTmdbClient client = new FakeTmdbClient();
    private final TmdbMovieDataProvider provider = new TmdbMovieDataProvider(client);

    private static TmdbSearchResult result(int id, String title, String year) {
        return result(id, title, year, 0);
    }

    private static TmdbSearchResult result(int id, String title, String year, int voteCount) {
        return new TmdbSearchResult(id, title, title, year + "-01-01", voteCount);
    }

    /** A result whose localized {@code title} differs from its {@code original_title}. */
    private static TmdbSearchResult localized(int id, String title, String originalTitle,
                                              String year, int voteCount) {
        return new TmdbSearchResult(id, title, originalTitle, year + "-01-01", voteCount);
    }

    private static TmdbSearchResponse search(TmdbSearchResult... results) {
        return new TmdbSearchResponse(List.of(results));
    }

    private static TmdbMovieDetails details(int id, String director) {
        return details(id, director, null, null);
    }

    private static TmdbMovieDetails details(int id, String director, Double voteAverage, Integer voteCount) {
        TmdbCredits credits = director == null ? null
                : new TmdbCredits(List.of(), List.of(new TmdbCrew(null, director, "Director")));
        return new TmdbMovieDetails(id, "tt" + id, "Title " + id, "Title " + id,
                "1999-01-01", 100, "overview", "/p.jpg", List.of(), List.of(), credits,
                voteAverage, voteCount);
    }

    private static Movie movieWithTmdbId(Integer tmdbId) {
        return new Movie(tmdbId, "tt" + tmdbId, "t", "t", null, null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
    }

    @Test
    void romanizesADirectorNameCreditedOnlyInANonLatinScript() throws Exception {
        TmdbCredits credits = new TmdbCredits(
                List.of(new TmdbMovieDetails.TmdbCast(1247, "송강호", 0)),
                List.of(new TmdbCrew(2376041, "이상용", "Director")));
        TmdbMovieDetails d = new TmdbMovieDetails(9, "tt9", "The Roundup", "범죄도시2",
                "2022-01-01", 100, "o", "/p.jpg", List.of(), List.of(), credits, null, null);
        client.onSearch("The Roundup", 2022, search(result(9, "The Roundup", "2022")));
        client.onDetails(9, d);
        client.onPerson(2376041, "Lee Sang-yong", List.of());
        client.onPerson(1247, "송강호", List.of("Song Kang-ho", "Song Kangho"));

        MovieMatch match = provider.identify(new MovieQuery("The Roundup", 2022, null));

        assertThat(match.movie().director().name()).isEqualTo("Lee Sang-yong");
        assertThat(match.movie().director().tmdbId()).isEqualTo(2376041);
        assertThat(match.movie().actors().get(0).name()).isEqualTo("Song Kang-ho"); // from also_known_as
    }

    @Test
    void keepsANonLatinNameWhenTmdbHasNoRomanizedSpellingAnywhere() throws Exception {
        TmdbCredits credits = new TmdbCredits(List.of(),
                List.of(new TmdbCrew(939147, "연상호", "Director")));
        TmdbMovieDetails d = new TmdbMovieDetails(8, "tt8", "Train to Busan", "부산행",
                "2016-01-01", 100, "o", "/p.jpg", List.of(), List.of(), credits, null, null);
        client.onSearch("Train to Busan", 2016, search(result(8, "Train to Busan", "2016")));
        client.onDetails(8, d);
        client.onPerson(939147, "연상호", List.of("ヨン・サンホ")); // no latin spelling at all

        MovieMatch match = provider.identify(new MovieQuery("Train to Busan", 2016, null));

        assertThat(match.movie().director().name()).isEqualTo("연상호"); // unchanged
    }

    @Test
    void doesNotLookUpPeopleWhoAlreadyHaveALatinName() throws Exception {
        client.onSearch("The Substance", 2024, search(result(1, "The Substance", "2024")));
        client.onDetails(1, details(1, "Coralie Fargeat")); // crew id is null here anyway
        // no onPerson(...) registered -> if the provider called personDetails it would NPE-free return null

        MovieMatch match = provider.identify(new MovieQuery("The Substance", 2024, null));

        assertThat(match.movie().director().name()).isEqualTo("Coralie Fargeat");
    }

    @Test
    void identifiesWhenExactlyOneSearchResult() throws Exception {
        client.onSearch("The Substance", 2024, search(result(1, "The Substance", "2024")));
        client.onDetails(1, details(1, "Coralie Fargeat"));

        MovieMatch match = provider.identify(new MovieQuery("The Substance", 2024, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(match.movie().tmdbId()).isEqualTo(1);
    }

    @Test
    void usesYearToDisambiguateMultipleResults() throws Exception {
        client.onSearch("Dune", 2021, search(
                result(1, "Dune", "1984"),
                result(2, "Dune", "2021"),
                result(3, "Dune", "2000")));
        client.onDetails(2, details(2, "Denis Villeneuve"));

        MovieMatch match = provider.identify(new MovieQuery("Dune", 2021, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(match.movie().tmdbId()).isEqualTo(2);
    }

    @Test
    void usesDirectorToDisambiguateWhenYearIsNotEnough() throws Exception {
        // Two movies with the same title and the same year: only the director separates them.
        client.onSearch("The Killer", 2023, search(
                result(10, "The Killer", "2023"),
                result(11, "The Killer", "2023")));
        client.onDetails(10, details(10, "David Fincher"));
        client.onDetails(11, details(11, "Someone Else"));

        MovieMatch match = provider.identify(new MovieQuery("The Killer", 2023, "David Fincher"));

        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(match.movie().tmdbId()).isEqualTo(10);
    }

    @Test
    void returnsAmbiguousWhenNeitherYearNorDirectorResolves() throws Exception {
        client.onSearch("Crash", null, search(
                result(1, "Crash", "1996"),
                result(2, "Crash", "2004")));

        MovieMatch match = provider.identify(new MovieQuery("Crash", null, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.AMBIGUOUS);
        assertThat(match.candidates()).containsExactly("Crash (1996)", "Crash (2004)");
        assertThat(match.movie()).isNull();
    }

    @Test
    void returnsAmbiguousWhenDirectorDoesNotMatchAnyCandidate() throws Exception {
        client.onSearch("The Killer", 2023, search(
                result(10, "The Killer", "2023"),
                result(11, "The Killer", "2023")));
        client.onDetails(10, details(10, "David Fincher"));
        client.onDetails(11, details(11, "John Woo"));

        MovieMatch match = provider.identify(new MovieQuery("The Killer", 2023, "Nobody Known"));

        assertThat(match.status()).isEqualTo(IdentificationStatus.AMBIGUOUS);
    }

    @Test
    void returnsNotFoundWhenNoResults() throws Exception {
        MovieMatch match = provider.identify(new MovieQuery("Unknown movie xyz", null, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.NOT_FOUND);
    }

    @Test
    void retriesWithoutYearWhenYearFilteredSearchReturnsNothing() throws Exception {
        client.onSearch("Oldboy", 2004, search());
        client.onSearch("Oldboy", null, search(result(11, "Oldboy", "2003")));
        client.onDetails(11, details(11, "Park Chan-wook"));

        MovieMatch match = provider.identify(new MovieQuery("Oldboy", 2004, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(match.movie().tmdbId()).isEqualTo(11);
    }

    @Test
    void wrapsProviderIoErrorsInMovieProviderException() {
        client.failSearch(new IOException("connection reset"));

        assertThatThrownBy(() -> provider.identify(new MovieQuery("Boom", null, null)))
                .isInstanceOf(MovieProviderException.class)
                .hasMessageContaining("Boom")
                .hasMessageContaining("connection reset");
    }

    // --- Letterboxd -> TMDB matching improvements --------------------------------

    @Test
    void matchesTheEnglishSearchTitleEvenWhenTheOriginalTitleIsNotLatin_parasite() throws Exception {
        // English search: the real film's `title` is "Parasite" (original_title is Korean);
        // "Parásitos" (its es-ES title) would not have matched. A different "Parasites" also shows.
        client.onSearch("Parasite", 2019, search(
                localized(603, "Parasite", "기생충", "2019", 15000),
                localized(2, "Parasites", "Parasites", "2019", 40)));
        client.onDetails(603, details(603, "Bong Joon-ho"));

        MovieMatch match = provider.identify(new MovieQuery("Parasite", 2019, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(match.movie().tmdbId()).isEqualTo(603);
    }

    @Test
    void stillMatchesViaTheOriginalTitleWhenTheSearchTitleDiffers() throws Exception {
        // Query is the film's original (non-English) title; TMDB `title` is the English one.
        client.onSearch("El hoyo", 2019, search(
                localized(619264, "The Platform", "El hoyo", "2019", 5000),
                result(9, "El Hoyo (short)", "2019", 1)));
        client.onDetails(619264, details(619264, "Galder Gaztelu-Urrutia"));

        MovieMatch match = provider.identify(new MovieQuery("El hoyo", 2019, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(match.movie().tmdbId()).isEqualTo(619264);
    }

    @Test
    void matchesTheEnglishTitleOverALocalizedOriginalTitle_thePlatform() throws Exception {
        client.onSearch("The Platform", 2019, search(
                localized(619264, "The Platform", "El hoyo", "2019", 5000),
                result(3, "The Platform Master", "2019", 5)));
        client.onDetails(619264, details(619264, "Galder Gaztelu-Urrutia"));

        MovieMatch match = provider.identify(new MovieQuery("The Platform", 2019, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(match.movie().tmdbId()).isEqualTo(619264);
    }

    @Test
    void resolvesDuplicateExactMatchesByVoteDominance_arrivalRockySplit() throws Exception {
        client.onSearch("Arrival", 2016, search(
                result(329865, "Arrival", "2016", 16000),
                result(9, "Arrival", "2016", 3)));
        client.onDetails(329865, details(329865, "Denis Villeneuve"));
        client.onSearch("Rocky", 1976, search(
                result(1366, "Rocky", "1976", 12000),
                result(4, "Rocky", "1976", 0)));
        client.onDetails(1366, details(1366, "John G. Avildsen"));
        client.onSearch("Split", 2016, search(
                result(381288, "Split", "2016", 14000),
                result(5, "Split", "2016", 2)));
        client.onDetails(381288, details(381288, "M. Night Shyamalan"));

        assertThat(provider.identify(new MovieQuery("Arrival", 2016, null)).movie().tmdbId())
                .isEqualTo(329865);
        assertThat(provider.identify(new MovieQuery("Rocky", 1976, null)).movie().tmdbId())
                .isEqualTo(1366);
        assertThat(provider.identify(new MovieQuery("Split", 2016, null)).movie().tmdbId())
                .isEqualTo(381288);
    }

    @Test
    void keepsAmbiguousWhenTwoRealFilmsShareTitleAndYear() throws Exception {
        // Two genuinely different films, both well-voted -> no clear dominance.
        client.onSearch("Crash", 2004, search(
                result(1, "Crash", "2004", 5000),
                result(2, "Crash", "2004", 4200)));

        MovieMatch match = provider.identify(new MovieQuery("Crash", 2004, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.AMBIGUOUS);
        assertThat(match.movie()).isNull();
    }

    @Test
    void ignoresPunctuationDifferencesInTitles_starWars() throws Exception {
        client.onSearch("Star Wars: Episode II – Attack of the Clones", 2002, search(
                result(1894, "Star Wars: Episode II - Attack of the Clones", "2002", 11000),
                result(8, "From Puppets to Pixels: Making Star Wars Episode II", "2002", 20)));
        client.onDetails(1894, details(1894, "George Lucas"));

        MovieMatch match = provider.identify(
                new MovieQuery("Star Wars: Episode II – Attack of the Clones", 2002, null));

        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(match.movie().tmdbId()).isEqualTo(1894);
    }

    // --- enrich (MovieEnricher) ----------------------------------------------------

    @Test
    void enrichAppliesTheTmdbRatingToAnIdentifiedMovie() {
        client.onDetails(42, details(42, null, 7.8, 9000));

        MovieEnrichment enrichment = provider.enrich(movieWithTmdbId(42));

        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.APPLIED);
        assertThat(enrichment.movie().ratings()).containsExactly(new Rating("TMDB", 7.8, 9000));
        assertThat(enrichment.error()).isNull();
    }

    @Test
    void enrichReturnsNoDataWhenTmdbHasNoUsableRating() {
        client.onDetails(42, details(42, null, 0.0, 0));
        Movie movie = movieWithTmdbId(42);

        MovieEnrichment enrichment = provider.enrich(movie);

        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.NO_DATA);
        assertThat(enrichment.movie()).isSameAs(movie);
    }

    @Test
    void enrichReturnsNoDataWhenTheMovieHasNoTmdbId() {
        Movie movie = movieWithTmdbId(null);

        MovieEnrichment enrichment = provider.enrich(movie);

        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.NO_DATA);
        assertThat(enrichment.movie()).isSameAs(movie);
    }

    @Test
    void enrichReturnsFailedWithoutThrowingWhenTheTmdbCallErrors() {
        client.failDetails(new IOException("gateway timeout"));
        Movie movie = movieWithTmdbId(42);

        MovieEnrichment enrichment = provider.enrich(movie);

        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.FAILED);
        assertThat(enrichment.movie()).isSameAs(movie);
        assertThat(enrichment.error()).contains("42").contains("gateway timeout");
    }

    @Test
    void enrichAppendsToAnyRatingsTheMovieAlreadyHas() {
        client.onDetails(42, details(42, null, 6.0, 10));
        Rating existing = new Rating("Critics", 88, 5);
        Movie movie = movieWithTmdbId(42).withRatings(List.of(existing));

        MovieEnrichment enrichment = provider.enrich(movie);

        assertThat(enrichment.movie().ratings())
                .containsExactly(existing, new Rating("TMDB", 6.0, 10));
    }

    private static final class FakeTmdbClient extends TmdbClient {

        private final Map<String, TmdbSearchResponse> searches = new HashMap<>();
        private final Map<Integer, TmdbMovieDetails> movies = new HashMap<>();
        private final Map<Integer, com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbPersonDetails> people = new HashMap<>();
        private IOException searchError;
        private IOException detailsError;

        private FakeTmdbClient() {
            super("test-key", "http://localhost/");
        }

        void onSearch(String title, Integer year, TmdbSearchResponse response) {
            searches.put(key(title, year), response);
        }

        void onDetails(int id, TmdbMovieDetails details) {
            movies.put(id, details);
        }

        void onPerson(int id, String name, java.util.List<String> alsoKnownAs) {
            people.put(id, new com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbPersonDetails(
                    id, name, alsoKnownAs));
        }

        void failSearch(IOException error) {
            this.searchError = error;
        }

        void failDetails(IOException error) {
            this.detailsError = error;
        }

        private static String key(String title, Integer year) {
            return title + "|" + year;
        }

        @Override
        public TmdbSearchResponse searchMovies(String title, Integer year) throws IOException {
            if (searchError != null) {
                throw searchError;
            }
            return searches.getOrDefault(key(title, year), new TmdbSearchResponse(List.of()));
        }

        @Override
        public TmdbMovieDetails movieDetails(int id) throws IOException {
            if (detailsError != null) {
                throw detailsError;
            }
            return movies.get(id);
        }

        @Override
        public com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbPersonDetails personDetails(int id) {
            return people.get(id);
        }
    }
}
