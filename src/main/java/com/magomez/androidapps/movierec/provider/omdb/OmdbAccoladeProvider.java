package com.magomez.androidapps.movierec.provider.omdb;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.AccoladeProvider;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * {@link AccoladeProvider} backed by OMDb's free-text {@code "Awards"} line, parsed by
 * {@link AwardsTallyParser} into an {@link AwardsTally}.
 *
 * <p>Looked up strictly by {@link Movie#imdbId()} (never by title, no scraping). OMDb
 * gives only the aggregate counts, so this provider fills {@link AccoladeReport#tally()}
 * and leaves {@link AccoladeReport#achievements()} empty — which festival gave what comes
 * from other providers. The {@link OmdbClient} response cache means this shares the fetch
 * with {@link OmdbMovieEnricher}.
 *
 * <ul>
 *   <li>no {@code imdbId}, OMDb miss, or no awards line &rarr; {@link AccoladeReport#empty()};</li>
 *   <li>API / HTTP error &rarr; logged, {@link AccoladeReport#empty()} (never throws).</li>
 * </ul>
 */
@Service
public class OmdbAccoladeProvider implements AccoladeProvider {

    private static final String SOURCE_NAME = "OMDb awards";

    private static final Logger log = LoggerFactory.getLogger(OmdbAccoladeProvider.class);

    private final OmdbClient client;

    public OmdbAccoladeProvider(OmdbClient client) {
        this.client = client;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public AccoladeReport accoladesOf(Movie movie) {
        if (movie == null || movie.imdbId() == null || movie.imdbId().isBlank()) {
            return AccoladeReport.empty();
        }
        try {
            OmdbResponse response = client.byImdbId(movie.imdbId());
            if (!response.isSuccess()) {
                return AccoladeReport.empty();
            }
            AwardsTally tally = AwardsTallyParser.parse(response.awards());
            return tally.isEmpty() ? AccoladeReport.empty()
                    : new AccoladeReport(tally, java.util.List.of());
        } catch (IOException e) {
            log.warn("OMDb awards lookup failed for imdbId {}: {}", movie.imdbId(), e.getMessage());
            return AccoladeReport.empty();
        }
    }
}
