package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.FestivalAchievement.Type;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.cannes.CannesArchiveEntry;
import com.magomez.androidapps.movierec.provider.cannes.CannesArchiveSource;
import com.magomez.androidapps.movierec.provider.cannes.CannesFestivalDataProvider;
import com.magomez.androidapps.movierec.provider.cannes.UnavailableCannesArchiveSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CannesFestivalDataProviderTest {

    private static Movie movie(String title, String releaseDate) {
        return new Movie(null, null, title, title, releaseDate, null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
    }

    /** In-memory {@link CannesArchiveSource}: no network. */
    private static CannesArchiveSource sourceReturning(CannesArchiveEntry... entries) {
        List<CannesArchiveEntry> rows = List.of(entries);
        return m -> rows;
    }

    @Test
    void sourceNameIsCannes() {
        assertThat(new CannesFestivalDataProvider(sourceReturning()).sourceName()).isEqualTo("Cannes");
    }

    @Test
    void mapsAnArchiveRowWithoutAnAwardToASelection() {
        CannesFestivalDataProvider provider = new CannesFestivalDataProvider(
                sourceReturning(new CannesArchiveEntry(2024, "Un Certain Regard", null)));

        List<FestivalAchievement> achievements = provider.achievementsOf(movie("The Movie", "2024-05-20"));

        assertThat(achievements).containsExactly(
                FestivalAchievement.selection("Cannes", 2024, "Un Certain Regard"));
        assertThat(achievements.get(0).type()).isEqualTo(Type.SELECTION);
    }

    @Test
    void mapsAnArchiveRowWithAnAwardToAnAward() {
        CannesFestivalDataProvider provider = new CannesFestivalDataProvider(
                sourceReturning(new CannesArchiveEntry(2019, "Compétition", "Palme d'Or")));

        List<FestivalAchievement> achievements = provider.achievementsOf(movie("Parasite", "2019-05-30"));

        assertThat(achievements).containsExactly(
                FestivalAchievement.award("Cannes", 2019, "Compétition", "Palme d'Or"));
        assertThat(achievements.get(0).type()).isEqualTo(Type.AWARD);
    }

    @Test
    void canRepresentBothASelectionAndAnAwardForTheSameMovie() {
        CannesFestivalDataProvider provider = new CannesFestivalDataProvider(sourceReturning(
                new CannesArchiveEntry(2024, "Un Certain Regard", null),
                new CannesArchiveEntry(2024, "Un Certain Regard", "Prix de la mise en scène")));

        List<FestivalAchievement> achievements = provider.achievementsOf(movie("The Movie", "2024-05-20"));

        assertThat(achievements).extracting(FestivalAchievement::type)
                .containsExactly(Type.SELECTION, Type.AWARD);
        assertThat(achievements).allSatisfy(a -> {
            assertThat(a.festival()).isEqualTo("Cannes");
            assertThat(a.editionYear()).isEqualTo(2024);
        });
    }

    @Test
    void returnsEmptyWhenTheSourceKnowsNothing() {
        CannesFestivalDataProvider provider = new CannesFestivalDataProvider(sourceReturning());

        assertThat(provider.achievementsOf(movie("Unknown", "2000-01-01"))).isEmpty();
    }

    @Test
    void withTheStillUnconnectedDefaultSourceItReturnsEmptyAndTouchesNoNetwork() {
        CannesFestivalDataProvider provider =
                new CannesFestivalDataProvider(new UnavailableCannesArchiveSource());

        assertThat(provider.achievementsOf(movie("Parasite", "2019-05-30"))).isEmpty();
    }

    @Test
    void rejectsANullMovie() {
        assertThatThrownBy(() -> new CannesFestivalDataProvider(sourceReturning()).achievementsOf(null))
                .isInstanceOf(NullPointerException.class);
    }
}
