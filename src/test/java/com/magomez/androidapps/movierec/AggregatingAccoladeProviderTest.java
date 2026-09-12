package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.AccoladeProvider;
import com.magomez.androidapps.movierec.provider.AggregatingAccoladeProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AggregatingAccoladeProvider}: the richer tally wins, festival achievements are
 * unioned across sources, and the result is order-independent.
 */
class AggregatingAccoladeProviderTest {

    private static Movie movie() {
        return new Movie(1, "tt1", "t", "t", null, null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
    }

    private static AccoladeProvider provider(String name, AccoladeReport report) {
        return new AccoladeProvider() {
            @Override
            public String sourceName() {
                return name;
            }

            @Override
            public AccoladeReport accoladesOf(Movie m) {
                return report;
            }
        };
    }

    @Test
    void noProvidersMeansAnEmptyReport() {
        assertThat(new AggregatingAccoladeProvider(List.of()).accoladesOf(movie()))
                .isEqualTo(AccoladeReport.empty());
    }

    @Test
    void mergesTheTallyFromOneSourceWithTheAchievementsFromAnother() {
        AccoladeProvider omdb = provider("omdb",
                new AccoladeReport(new AwardsTally(0, 0, 4, 9), List.of()));
        AccoladeProvider wikidata = provider("wikidata", new AccoladeReport(AwardsTally.empty(),
                List.of(FestivalAchievement.award("Sitges", 2025, "Oficial Fantàstic", "Millor Film"))));

        AccoladeReport merged = new AggregatingAccoladeProvider(List.of(omdb, wikidata)).accoladesOf(movie());

        assertThat(merged.tally()).isEqualTo(new AwardsTally(0, 0, 4, 9));
        assertThat(merged.achievements()).hasSize(1);
    }

    @Test
    void isOrderIndependent() {
        AccoladeReport a = new AccoladeReport(new AwardsTally(0, 0, 2, 2),
                List.of(FestivalAchievement.selection("Sundance", 2026, "Midnight")));
        AccoladeReport b = new AccoladeReport(new AwardsTally(0, 0, 10, 20),
                List.of(FestivalAchievement.selection("Fantasia", 2026, "Camera Lucida")));

        AccoladeReport ab = new AggregatingAccoladeProvider(
                List.of(provider("a", a), provider("b", b))).accoladesOf(movie());
        AccoladeReport ba = new AggregatingAccoladeProvider(
                List.of(provider("b", b), provider("a", a))).accoladesOf(movie());

        assertThat(ab.tally()).isEqualTo(new AwardsTally(0, 0, 10, 20)); // richer wins either way
        assertThat(ba.tally()).isEqualTo(new AwardsTally(0, 0, 10, 20));
        assertThat(ab.achievements()).containsExactlyInAnyOrderElementsOf(ba.achievements());
    }
}
