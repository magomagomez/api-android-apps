package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.provider.festival.FestivalLineup;
import com.magomez.androidapps.movierec.provider.festival.LineupEntry;
import com.magomez.androidapps.movierec.provider.festival.wikipedia.WikipediaClient;
import com.magomez.androidapps.movierec.provider.festival.wikipedia.WikipediaFestivalLineupSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WikipediaFestivalLineupSource}: pulls film titles + sections out of the
 * {@code wikitable}s on a festival-edition article, liberally (non-film rows are
 * harmless because titles are matched, not trusted).
 */
class WikipediaFestivalLineupSourceTest {

    // Mimics modern parsed Wikipedia HTML: headings inside div.mw-heading, films in wikitables.
    private static final String PAGE_HTML = """
            <div class="mw-parser-output">
              <div class="mw-heading mw-heading2"><h2 id="Competition">Dramatic Competition</h2></div>
              <table class="wikitable">
                <tbody>
                  <tr><th>Title</th><th>Director(s)</th></tr>
                  <tr><td><i><a href="/wiki/A">The Assessment</a></i></td><td>Fleur Fortuné</td></tr>
                  <tr><td><i>Sorry, Baby</i></td><td>Eva Victor</td></tr>
                </tbody>
              </table>
              <div class="mw-heading mw-heading2"><h2 id="Midnight">Midnight</h2></div>
              <table class="wikitable">
                <tr><th>Title</th></tr>
                <tr><td><a href="/wiki/B">Crawlers</a></td><td>Foo</td></tr>
              </table>
              <div class="mw-heading mw-heading2"><h2 id="Juries">Juries</h2></div>
              <table class="wikitable">
                <tr><th>Member</th></tr>
                <tr><td>Some Juror</td></tr>
              </table>
            </div>""";

    private final FakeClient client = new FakeClient();
    private final WikipediaFestivalLineupSource source = new WikipediaFestivalLineupSource(client);

    @Test
    void extractsTitlesWithTheirSectionFromEveryWikitable() throws IOException {
        client.html = Optional.of(PAGE_HTML);

        Optional<FestivalLineup> lineup = source.lineup("Sundance", 2026);

        assertThat(lineup).isPresent();
        assertThat(lineup.get().festival()).isEqualTo("Sundance");
        assertThat(lineup.get().year()).isEqualTo(2026);
        assertThat(lineup.get().entries()).contains(
                new LineupEntry("The Assessment", "Dramatic Competition"),
                new LineupEntry("Sorry, Baby", "Dramatic Competition"),
                new LineupEntry("Crawlers", "Midnight"),
                new LineupEntry("Some Juror", "Juries")); // noise kept — never matches a candidate
    }

    @Test
    void requestsThePageTitleForTheFestivalEdition() throws IOException {
        client.html = Optional.of(PAGE_HTML);

        source.lineup("Sundance", 2026);

        assertThat(client.lastPage).isEqualTo("2026 Sundance Film Festival");
    }

    @Test
    void mapsKnownFestivalKeysToTheirArticleName() throws IOException {
        client.html = Optional.of(PAGE_HTML);

        source.lineup("SXSW", 2026);
        assertThat(client.lastPage).isEqualTo("2026 South by Southwest");

        source.lineup("Berlinale", 2026);
        assertThat(client.lastPage).isEqualTo("2026 Berlin International Film Festival");
    }

    @Test
    void aMissingPageIsAnEmptyLineup() throws IOException {
        client.html = Optional.empty();

        assertThat(source.lineup("Nonexistent Fest", 2026)).isEmpty();
    }

    private static final class FakeClient extends WikipediaClient {

        private Optional<String> html = Optional.empty();
        private String lastPage;

        private FakeClient() {
            super("http://localhost/w/api.php", "test-agent");
        }

        @Override
        public Optional<String> pageHtml(String pageTitle) {
            this.lastPage = pageTitle;
            return html;
        }
    }
}
