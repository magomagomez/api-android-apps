package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTasteProfileTest {

    @Test
    void emptyHasNoPreferencesAndZeroAffinities() {
        UserTasteProfile p = UserTasteProfile.empty();

        assertThat(p.preferredDirectors()).isEmpty();
        assertThat(p.preferredGenres()).isEmpty();
        assertThat(p.preferredActors()).isEmpty();
        assertThat(p.preferredCountries()).isEmpty();
        assertThat(p.preferredDecades()).isEmpty();
        assertThat(p.directorAffinityOf("anyone")).isZero();
        assertThat(p.decadeAffinityOf(2020)).isZero();
    }

    @Test
    void theSetConstructorAssignsAUniformWeightOfOne() {
        UserTasteProfile p = new UserTasteProfile(
                Set.of("Nolan"), Set.of("Sci-Fi", "Drama"), Set.of("Bale"),
                Set.of("USA"), Set.of(2010, 2000));

        assertThat(p.preferredGenres()).containsExactlyInAnyOrder("Sci-Fi", "Drama");
        assertThat(p.directorAffinityOf("Nolan")).isEqualTo(1.0);
        assertThat(p.genreAffinityOf("Sci-Fi")).isEqualTo(1.0);
        assertThat(p.decadeAffinityOf(2010)).isEqualTo(1.0);
    }

    @Test
    void preferredViewsAreTheMapKeysAndAreUnmodifiable() {
        UserTasteProfile p = new UserTasteProfile(
                Map.of("Nolan", 1.0), Map.of("Sci-Fi", 0.5), Map.of(), Map.of(), Map.of(2010, 1.0));

        assertThat(p.preferredDirectors()).isEqualTo(p.directorAffinity().keySet());
        assertThatThrownBy(() -> p.preferredGenres().add("Drama"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> p.decadeAffinity().put(2020, 1.0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void affinityLookupsReturnZeroForUnknownOrNull() {
        UserTasteProfile p = new UserTasteProfile(
                Map.of("Nolan", 0.8), Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(p.directorAffinityOf("Nolan")).isEqualTo(0.8);
        assertThat(p.directorAffinityOf("Someone Else")).isZero();
        assertThat(p.directorAffinityOf((String) null)).isZero();
        assertThat(p.directorAffinityOf((com.magomez.androidapps.movierec.model.Director) null)).isZero();
        assertThat(p.decadeAffinityOf(1975)).isZero();
    }

    @Test
    void rejectsAnAffinityOutsideZeroToOne() {
        assertThatThrownBy(() -> new UserTasteProfile(Map.of("X", 1.5), Map.of(), Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserTasteProfile(Map.of("X", -0.1), Map.of(), Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserTasteProfile(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(2020, Double.NaN)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isValueBased() {
        UserTasteProfile a = new UserTasteProfile(Set.of("Nolan"), Set.of("Sci-Fi"), Set.of(), Set.of(), Set.of(2010));
        UserTasteProfile b = new UserTasteProfile(Set.of("Nolan"), Set.of("Sci-Fi"), Set.of(), Set.of(), Set.of(2010));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(builderStyle()).isEqualTo(builderStyle());
    }

    private static UserTasteProfile builderStyle() {
        return new UserTasteProfile(Map.of("Nolan", 0.5, "Villeneuve", 1.0), Map.of("Sci-Fi", 1.0),
                Map.of(), Map.of(), Map.of(2010, 0.25, 2020, 1.0));
    }
}
