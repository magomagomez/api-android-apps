package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.festival.FestivalLineup;
import com.magomez.androidapps.movierec.provider.festival.FestivalLineupAccoladeProvider;
import com.magomez.androidapps.movierec.provider.festival.FestivalLineupSource;
import com.magomez.androidapps.movierec.provider.festival.LineupEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link FestivalLineupAccoladeProvider}: title cross-reference against configured lineups. */
class FestivalLineupAccoladeProviderTest {

    private static Movie movie(String title, String originalTitle) {
        return new Movie(1, "tt1", title, originalTitle, "2026-01-01", null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
    }

    private final FakeSource source = new FakeSource(Map.of(
            "Sundance:2026", new FestivalLineup("Sundance", 2026, List.of(
                    new LineupEntry("The Cycle", "Midnight"),
                    new LineupEntry("Sorry, Baby", "U.S. Dramatic Competition"))),
            "Fantasia:2026", new FestivalLineup("Fantasia", 2026, List.of(
                    new LineupEntry("The Cycle", "Camera Lucida")))));

    private FestivalLineupAccoladeProvider provider(String configured) {
        return new FestivalLineupAccoladeProvider(source, configured);
    }

    @Test
    void aCandidateInTwoLineupsGetsASelectionForEach() {
        AccoladeReport report = provider("Sundance:2026,Fantasia:2026").accoladesOf(movie("The Cycle", "The Cycle"));

        assertThat(report.achievements()).containsExactlyInAnyOrder(
                FestivalAchievement.selection("Sundance", 2026, "Midnight"),
                FestivalAchievement.selection("Fantasia", 2026, "Camera Lucida"));
    }

    @Test
    void matchesOnTheOriginalTitleToo() {
        AccoladeReport report = provider("Sundance:2026").accoladesOf(movie("Perdona, cariño", "Sorry, Baby"));

        assertThat(report.achievements()).containsExactly(
                FestivalAchievement.selection("Sundance", 2026, "U.S. Dramatic Competition"));
    }

    @Test
    void aCandidateInNoLineupGetsNothing() {
        assertThat(provider("Sundance:2026,Fantasia:2026").accoladesOf(movie("Some Other Film", "Some Other Film")))
                .isEqualTo(AccoladeReport.empty());
    }

    @Test
    void lineupsAreFetchedOnceAndReused() {
        FestivalLineupAccoladeProvider provider = provider("Sundance:2026");

        provider.accoladesOf(movie("A", "A"));
        provider.accoladesOf(movie("B", "B"));
        provider.accoladesOf(movie("The Cycle", "The Cycle"));

        assertThat(source.calls).hasValue(1); // one edition, fetched a single time
    }

    @Test
    void configuredEditionsAreDeduplicatedAndBadTokensIgnored() {
        FestivalLineupAccoladeProvider provider = provider("Sundance:2026, Sundance:2026 , junk, Fantasia:notayear");

        provider.accoladesOf(movie("The Cycle", "The Cycle"));

        assertThat(source.calls).hasValue(1);
    }

    @Test
    void aSourceFailureForOneEditionIsSkipped() {
        FakeSource failing = new FakeSource(Map.of()) {
            @Override
            public Optional<FestivalLineup> lineup(String festival, int year) throws IOException {
                throw new IOException("wikipedia down");
            }
        };

        assertThat(new FestivalLineupAccoladeProvider(failing, "Sundance:2026")
                .accoladesOf(movie("The Cycle", "The Cycle"))).isEqualTo(AccoladeReport.empty());
    }

    private static class FakeSource implements FestivalLineupSource {

        private final Map<String, FestivalLineup> byKey;
        private final AtomicInteger calls = new AtomicInteger();

        private FakeSource(Map<String, FestivalLineup> byKey) {
            this.byKey = byKey;
        }

        @Override
        public Optional<FestivalLineup> lineup(String festival, int year) throws IOException {
            calls.incrementAndGet();
            return Optional.ofNullable(byKey.get(festival + ":" + year));
        }
    }
}
