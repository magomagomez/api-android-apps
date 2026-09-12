package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.wikidata.WikidataAccoladeProvider;
import com.magomez.androidapps.movierec.provider.wikidata.WikidataAward;
import com.magomez.androidapps.movierec.provider.wikidata.WikidataClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Hand-written {@link WikidataClient} double; no test hits the real SPARQL endpoint. */
class WikidataAccoladeProviderTest {

    private final FakeWikidataClient client = new FakeWikidataClient();
    private final WikidataAccoladeProvider provider = new WikidataAccoladeProvider(client);

    private static Movie movie(String imdbId) {
        return new Movie(1, imdbId, "t", "t", null, null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
    }

    @Test
    void mapsAnAwardWonToAFestivalAwardAchievement() {
        client.awards = List.of(new WikidataAward(true, "Palme d'Or", null, 2024, 2024));

        AccoladeReport report = provider.accoladesOf(movie("tt1"));

        assertThat(report.achievements()).containsExactly(
                FestivalAchievement.award("Cannes", 2024, null, "Palme d'Or"));
        assertThat(report.tally().isEmpty()).isTrue(); // OMDb owns the tally
    }

    @Test
    void mapsAFestivalNominationToACompetitionSelection() {
        client.awards = List.of(new WikidataAward(false, "Golden Bear", null, 2025, 2025));

        assertThat(provider.accoladesOf(movie("tt1")).achievements()).containsExactly(
                FestivalAchievement.selection("Berlinale", 2025, "Competición"));
    }

    @Test
    void prefersTheConferrerLabelOverTheAwardNameMap() {
        client.awards = List.of(new WikidataAward(
                true, "Best Film", "Sitges Film Festival", 2026, 2026));

        assertThat(provider.accoladesOf(movie("tt1")).achievements()).containsExactly(
                FestivalAchievement.award("Sitges Film Festival", 2026, null, "Best Film"));
    }

    @Test
    void fallsBackToThePublicationYearWhenTheStatementHasNoYear() {
        client.awards = List.of(new WikidataAward(true, "Golden Lion", null, null, 2023));

        assertThat(provider.accoladesOf(movie("tt1")).achievements()).containsExactly(
                FestivalAchievement.award("Venecia", 2023, null, "Golden Lion"));
    }

    @Test
    void dropsAFactThatCannotBePlacedAtAFestival() {
        client.awards = List.of(
                new WikidataAward(true, "Some Local Critics Prize", null, 2024, 2024),
                new WikidataAward(true, "Golden Bear", null, null, null)); // no year at all

        assertThat(provider.accoladesOf(movie("tt1"))).isEqualTo(AccoladeReport.empty());
    }

    @Test
    void dropsAcademyAndBaftaCategoriesSinceOmdbAlreadyCountsThose() {
        client.awards = List.of(
                new WikidataAward(false, "Academy Award for Best Picture",
                        "Academy of Motion Picture Arts and Sciences", 2020, 2019),
                new WikidataAward(true, "BAFTA Award for Best Film",
                        "British Academy of Film and Television Arts", 2020, 2019));

        assertThat(provider.accoladesOf(movie("tt1"))).isEqualTo(AccoladeReport.empty());
    }

    @Test
    void deduplicatesRepeatedRows() {
        client.awards = List.of(
                new WikidataAward(true, "Golden Lion", null, 2024, 2024),
                new WikidataAward(true, "Golden Lion", null, 2024, 2024));

        assertThat(provider.accoladesOf(movie("tt1")).achievements()).hasSize(1);
    }

    @Test
    void noImdbIdMeansAnEmptyReportAndNoLookup() {
        assertThat(provider.accoladesOf(movie(null))).isEqualTo(AccoladeReport.empty());
        assertThat(client.called).isFalse();
    }

    @Test
    void aClientErrorIsSwallowedAsAnEmptyReport() {
        client.error = new IOException("HTTP 429");

        assertThat(provider.accoladesOf(movie("tt1"))).isEqualTo(AccoladeReport.empty());
    }

    private static final class FakeWikidataClient extends WikidataClient {

        private List<WikidataAward> awards = List.of();
        private IOException error;
        private boolean called;

        private FakeWikidataClient() {
            super("http://localhost/sparql", "test-agent");
        }

        @Override
        public List<WikidataAward> awardsForImdbId(String imdbId) throws IOException {
            called = true;
            if (error != null) {
                throw error;
            }
            return awards;
        }
    }
}
