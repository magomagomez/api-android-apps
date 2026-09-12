package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.provider.festival.FestivalLineup;
import com.magomez.androidapps.movierec.provider.festival.LineupEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link FestivalLineup}: title lookup is accent- and punctuation-insensitive. */
class FestivalLineupTest {

    private final FestivalLineup lineup = new FestivalLineup("Sitges", 2026, List.of(
            new LineupEntry("The Substance", "Òrbita"),
            new LineupEntry("¡Átame!", "Retrospectiva")));

    @Test
    void looksUpIgnoringCasePunctuationAndAccents() {
        assertThat(lineup.lookup("the substance")).map(LineupEntry::section).contains("Òrbita");
        assertThat(lineup.lookup("THE  SUBSTANCE!")).isPresent();
        assertThat(lineup.lookup("Atame")).isPresent();
    }

    @Test
    void returnsEmptyForAMissOrABlankQuery() {
        assertThat(lineup.lookup("Nope")).isEmpty();
        assertThat(lineup.lookup("")).isEmpty();
        assertThat(lineup.lookup(null)).isEmpty();
    }
}
