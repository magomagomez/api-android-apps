package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;
import com.magomez.androidapps.movierec.model.MovieIdentificationResult;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.MovieDataProvider;
import com.magomez.androidapps.movierec.provider.MovieEnricher;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.service.MovieImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieImportServiceTest {

    @Mock
    private MovieDataProvider provider;

    private MovieImportService service(MovieEnricher... enrichers) {
        return new MovieImportService(provider, List.of(enrichers));
    }

    private static Movie sampleMovie(String title) {
        return new Movie(1, "tt1", title, title, "1999-01-01", 100, "o",
                List.of(), Director.of("dir"), List.of(), List.of(), null, List.of());
    }

    /** Enricher that appends a rating, tracking whether it was invoked. */
    private static MovieEnricher addsRating(Rating rating, AtomicInteger calls) {
        return new MovieEnricher() {
            @Override
            public MovieEnrichment enrich(Movie movie) {
                calls.incrementAndGet();
                List<Rating> merged = new ArrayList<>(movie.ratings());
                merged.add(rating);
                return MovieEnrichment.applied(movie.withRatings(merged));
            }

            @Override
            public String name() {
                return rating.source();
            }
        };
    }

    private static MovieEnricher noData() {
        return simpleEnricher("no-data", MovieEnrichment::noData);
    }

    private static MovieEnricher fails(String error) {
        return simpleEnricher("failing", m -> MovieEnrichment.failed(m, error));
    }

    private static MovieEnricher simpleEnricher(String name, java.util.function.Function<Movie, MovieEnrichment> body) {
        return new MovieEnricher() {
            @Override
            public MovieEnrichment enrich(Movie movie) {
                return body.apply(movie);
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    @Test
    void identifiesEachQueryPreservingOrderAndRunsEnrichersOnlyForIdentified() throws Exception {
        MovieQuery identified = new MovieQuery("Identified", null, null);
        MovieQuery missing = new MovieQuery("Missing", null, null);
        MovieQuery ambiguous = new MovieQuery("Ambiguous", null, null);
        when(provider.identify(identified))
                .thenReturn(MovieMatch.identified(sampleMovie("Identified")));
        when(provider.identify(missing)).thenReturn(MovieMatch.notFound());
        when(provider.identify(ambiguous))
                .thenReturn(MovieMatch.ambiguous(List.of("Ambiguous (1996)")));
        AtomicInteger calls = new AtomicInteger();

        List<MovieIdentificationResult> results = service(addsRating(new Rating("X", 5.0, 1), calls))
                .identifyAll(List.of(identified, missing, ambiguous));

        assertThat(results).extracting(r -> r.match().status()).containsExactly(
                IdentificationStatus.IDENTIFIED,
                IdentificationStatus.NOT_FOUND,
                IdentificationStatus.AMBIGUOUS);
        assertThat(calls.get()).isEqualTo(1); // only the identified one was enriched
        assertThat(results).allSatisfy(r -> {
            assertThat(r.identificationError()).isNull();
            assertThat(r.enrichmentError()).isNull();
        });
    }

    @Test
    void passesTheDomainQueryUnchangedToTheProvider() throws Exception {
        MovieQuery query = new MovieQuery("The Substance", 2024, "Coralie Fargeat");
        when(provider.identify(any())).thenReturn(MovieMatch.notFound());

        service().identifyAll(List.of(query));

        ArgumentCaptor<MovieQuery> captor = ArgumentCaptor.forClass(MovieQuery.class);
        verify(provider).identify(captor.capture());
        assertThat(captor.getValue()).isEqualTo(query);
    }

    @Test
    void identificationErrorForOneQueryBecomesANotFoundResultCarryingTheMessage() throws Exception {
        MovieQuery good = new MovieQuery("Good", null, null);
        MovieQuery bad = new MovieQuery("Bad", null, null);
        when(provider.identify(good)).thenReturn(MovieMatch.identified(sampleMovie("Good")));
        when(provider.identify(bad)).thenThrow(new MovieProviderException("TMDB down"));

        List<MovieIdentificationResult> results = service(noData()).identifyAll(List.of(good, bad));

        assertThat(results.get(0).match().status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(results.get(0).identificationError()).isNull();
        assertThat(results.get(1).match().status()).isEqualTo(IdentificationStatus.NOT_FOUND);
        assertThat(results.get(1).identificationError()).isEqualTo("TMDB down");
        assertThat(results.get(1).enrichmentError()).isNull();
    }

    @Test
    void emptyQueryListProducesNoResultsAndNoProviderCalls() {
        assertThat(service().identifyAll(List.of())).isEmpty();
        verifyNoInteractions(provider);
    }

    // --- enrichment ----------------------------------------------------------------

    @Test
    void enrichesAnIdentifiedMovie() throws Exception {
        MovieQuery query = new MovieQuery("The Substance", 2024, null);
        Rating rating = new Rating("TMDB", 7.8, 9000);
        when(provider.identify(query))
                .thenReturn(MovieMatch.identified(sampleMovie("The Substance")));

        MovieIdentificationResult result = service(addsRating(rating, new AtomicInteger()))
                .identifyAll(List.of(query)).get(0);

        assertThat(result.match().status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(result.match().movie().ratings()).containsExactly(rating);
        assertThat(result.enrichmentError()).isNull();
    }

    @Test
    void enricherWithoutDataLeavesTheMovieUnchanged() throws Exception {
        MovieQuery query = new MovieQuery("Obscure", null, null);
        when(provider.identify(query)).thenReturn(MovieMatch.identified(sampleMovie("Obscure")));

        MovieIdentificationResult result = service(noData()).identifyAll(List.of(query)).get(0);

        assertThat(result.match().status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(result.match().movie().ratings()).isEmpty();
        assertThat(result.enrichmentError()).isNull();
    }

    @Test
    void enricherFailureKeepsTheMovieIdentifiedAndRecordsTheErrorSeparately() throws Exception {
        MovieQuery query = new MovieQuery("The Substance", 2024, null);
        when(provider.identify(query))
                .thenReturn(MovieMatch.identified(sampleMovie("The Substance")));

        MovieIdentificationResult result = service(fails("rating endpoint 500"))
                .identifyAll(List.of(query)).get(0);

        assertThat(result.match().status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(result.match().movie().title()).isEqualTo("The Substance");
        assertThat(result.match().movie().ratings()).isEmpty();
        assertThat(result.identificationError()).isNull();
        assertThat(result.enrichmentError()).isEqualTo("rating endpoint 500");
    }

    @Test
    void appliesSeveralEnrichersSequentiallyPreservingOrderAndMovieIdentity() throws Exception {
        MovieQuery query = new MovieQuery("The Substance", 2024, null);
        Movie identified = sampleMovie("The Substance");
        when(provider.identify(query)).thenReturn(MovieMatch.identified(identified));
        Rating first = new Rating("TMDB", 7.8, 9000);
        Rating second = new Rating("Critics", 90, 12);

        MovieIdentificationResult result = service(
                addsRating(first, new AtomicInteger()),
                addsRating(second, new AtomicInteger()))
                .identifyAll(List.of(query)).get(0);

        Movie enriched = result.match().movie();
        assertThat(enriched.ratings()).containsExactly(first, second); // order of enrichers preserved
        assertThat(enriched.tmdbId()).isEqualTo(identified.tmdbId());
        assertThat(enriched.imdbId()).isEqualTo(identified.imdbId());
        assertThat(enriched.title()).isEqualTo(identified.title());
        assertThat(result.enrichmentError()).isNull();
    }

    @Test
    void reportsEveryFailingEnricherWhileStillApplyingTheSuccessfulOnes() throws Exception {
        MovieQuery query = new MovieQuery("The Substance", 2024, null);
        when(provider.identify(query))
                .thenReturn(MovieMatch.identified(sampleMovie("The Substance")));
        Rating ok = new Rating("TMDB", 7.8, 9000);

        MovieIdentificationResult result = service(
                fails("boom A"),
                addsRating(ok, new AtomicInteger()),
                fails("boom B"))
                .identifyAll(List.of(query)).get(0);

        assertThat(result.match().status()).isEqualTo(IdentificationStatus.IDENTIFIED);
        assertThat(result.match().movie().ratings()).containsExactly(ok);
        assertThat(result.enrichmentError()).isEqualTo("boom A; boom B");
    }

    @Test
    void preservesInputOrderWhileEnriching() throws Exception {
        MovieQuery a = new MovieQuery("A", null, null);
        MovieQuery b = new MovieQuery("B", null, null);
        MovieQuery c = new MovieQuery("C", null, null);
        when(provider.identify(a)).thenReturn(MovieMatch.identified(sampleMovie("A")));
        when(provider.identify(b)).thenReturn(MovieMatch.notFound());
        when(provider.identify(c)).thenReturn(MovieMatch.identified(sampleMovie("C")));

        List<MovieIdentificationResult> results = service(addsRating(new Rating("TMDB", 6.0, 10), new AtomicInteger()))
                .identifyAll(List.of(a, b, c));

        assertThat(results).extracting(r -> r.query().title()).containsExactly("A", "B", "C");
        assertThat(results.get(0).match().movie().ratings()).hasSize(1);
        assertThat(results.get(1).match().movie()).isNull();
        assertThat(results.get(2).match().movie().ratings()).hasSize(1);
    }
}
