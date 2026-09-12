package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.provider.omdb.AwardsTallyParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AwardsTallyParser}: the many shapes of OMDb's {@code "Awards"} line, and the
 * junk that must fall back to {@link AwardsTally#empty()}.
 */
class AwardsTallyParserTest {

    @Test
    void oscarWinWithTotalsTail() {
        AwardsTally tally = AwardsTallyParser.parse("Won 1 Oscar. 9 wins & 23 nominations total");

        assertThat(tally).isEqualTo(new AwardsTally(1, 0, 9, 23));
        assertThat(tally.hasOscar()).isTrue();
    }

    @Test
    void oscarNominationWithTotalsTail() {
        assertThat(AwardsTallyParser.parse("Nominated for 1 Oscar. 5 wins & 8 nominations total"))
                .isEqualTo(new AwardsTally(0, 1, 5, 8));
    }

    @Test
    void nonOscarPrefixIsNotCountedAsAnOscar() {
        assertThat(AwardsTallyParser.parse("Nominated for 3 BAFTA Awards. 10 wins & 30 nominations total"))
                .isEqualTo(new AwardsTally(0, 0, 10, 30));
    }

    @Test
    void plainTotalsWithoutAPrefix() {
        assertThat(AwardsTallyParser.parse("1 win & 2 nominations"))
                .isEqualTo(new AwardsTally(0, 0, 1, 2));
    }

    @Test
    void prefixOnlyWithNoTotalsTailUsesThePrefixCount() {
        assertThat(AwardsTallyParser.parse("Won 2 Primetime Emmys."))
                .isEqualTo(new AwardsTally(0, 0, 2, 0));
        assertThat(AwardsTallyParser.parse("Nominated for 2 Golden Globes."))
                .isEqualTo(new AwardsTally(0, 0, 0, 2));
    }

    @Test
    void oscarCountIsClampedIntoTheTotalWhenTheLineIsInconsistent() {
        AwardsTally tally = AwardsTallyParser.parse("Won 3 Oscars. 2 wins & 1 nomination");

        assertThat(tally.oscarWins()).isEqualTo(3);
        assertThat(tally.wins()).isEqualTo(3); // never below the Oscar count
    }

    @Test
    void missingOrJunkYieldsEmpty() {
        assertThat(AwardsTallyParser.parse(null)).isEqualTo(AwardsTally.empty());
        assertThat(AwardsTallyParser.parse("")).isEqualTo(AwardsTally.empty());
        assertThat(AwardsTallyParser.parse("   ")).isEqualTo(AwardsTally.empty());
        assertThat(AwardsTallyParser.parse("N/A")).isEqualTo(AwardsTally.empty());
        assertThat(AwardsTallyParser.parse("A well-regarded film")).isEqualTo(AwardsTally.empty());
    }

    @Test
    void isDeterministic() {
        String line = "Won 2 Oscars. 50 wins & 120 nominations total";
        assertThat(AwardsTallyParser.parse(line)).isEqualTo(AwardsTallyParser.parse(line));
    }
}
