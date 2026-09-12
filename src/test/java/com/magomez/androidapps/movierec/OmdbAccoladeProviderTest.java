package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.omdb.OmdbAccoladeProvider;
import com.magomez.androidapps.movierec.provider.omdb.OmdbClient;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Hand-written {@link OmdbClient} double; no test hits the real OMDb API. */
class OmdbAccoladeProviderTest {

    private final FakeOmdbClient client = new FakeOmdbClient();
    private final OmdbAccoladeProvider provider = new OmdbAccoladeProvider(client);

    private static Movie movie(String imdbId) {
        return new Movie(1, imdbId, "t", "t", null, null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
    }

    private static OmdbResponse success(String awards) {
        return new OmdbResponse("True", null, "7.0", "1000", "60", awards, List.of());
    }

    @Test
    void parsesTheOmdbAwardsLineIntoATally() {
        client.answer = success("Won 1 Oscar. 9 wins & 23 nominations total");

        AccoladeReport report = provider.accoladesOf(movie("tt1"));

        assertThat(report.tally()).isEqualTo(new AwardsTally(1, 0, 9, 23));
        assertThat(report.achievements()).isEmpty(); // OMDb gives no per-festival detail
    }

    @Test
    void noImdbIdMeansNoLookupAndAnEmptyReport() {
        assertThat(provider.accoladesOf(movie(null))).isEqualTo(AccoladeReport.empty());
        assertThat(provider.accoladesOf(movie("  "))).isEqualTo(AccoladeReport.empty());
        assertThat(client.called).isFalse();
    }

    @Test
    void anOmdbMissIsAnEmptyReport() {
        client.answer = new OmdbResponse("False", "Incorrect IMDb ID.", null, null, null, null, null);

        assertThat(provider.accoladesOf(movie("tt1"))).isEqualTo(AccoladeReport.empty());
    }

    @Test
    void aNaAwardsLineIsAnEmptyReport() {
        client.answer = success("N/A");

        assertThat(provider.accoladesOf(movie("tt1"))).isEqualTo(AccoladeReport.empty());
    }

    @Test
    void anOmdbErrorIsSwallowedAsAnEmptyReport() {
        client.error = new IOException("HTTP 500");

        assertThat(provider.accoladesOf(movie("tt1"))).isEqualTo(AccoladeReport.empty());
    }

    private static final class FakeOmdbClient extends OmdbClient {

        private OmdbResponse answer;
        private IOException error;
        private boolean called;

        private FakeOmdbClient() {
            super("test-key", "http://localhost/");
        }

        @Override
        public OmdbResponse byImdbId(String imdbId) throws IOException {
            called = true;
            if (error != null) {
                throw error;
            }
            return answer;
        }
    }
}
