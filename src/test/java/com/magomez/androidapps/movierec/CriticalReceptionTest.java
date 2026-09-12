package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.CriticalReception;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriticalReceptionTest {

    @Test
    void holdsTheReportedValuesUntouched() {
        CriticalReception reception =
                new CriticalReception("Screen Jury Grid", "Cannes", 2019, 3.2, 4.0, 12);

        assertThat(reception.source()).isEqualTo("Screen Jury Grid");
        assertThat(reception.festival()).isEqualTo("Cannes");
        assertThat(reception.editionYear()).isEqualTo(2019);
        assertThat(reception.score()).isEqualTo(3.2);
        assertThat(reception.scale()).isEqualTo(4.0);
        assertThat(reception.criticCount()).isEqualTo(12);
        assertThat(reception.hasCritics()).isTrue();
    }

    @Test
    void isImmutableAndValueBased() {
        assertThat(CriticalReception.class.isRecord()).isTrue();

        CriticalReception a = CriticalReception.of("IndieWire", "Cannes", 2021, 78, 100, 30);
        CriticalReception b = CriticalReception.of("IndieWire", "Cannes", 2021, 78, 100, 30);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(CriticalReception.of("IndieWire", "Cannes", 2021, 79, 100, 30));
    }

    @Test
    void trimsSourceAndFestival() {
        CriticalReception reception =
                new CriticalReception("  Jury Grid  ", "  Cannes  ", 2024, 2.0, 4.0, 0);

        assertThat(reception.source()).isEqualTo("Jury Grid");
        assertThat(reception.festival()).isEqualTo("Cannes");
    }

    @Test
    void acceptsBoundaryValues() {
        assertThatCode(() -> new CriticalReception("s", "f", 1900, 0.0, 0.001, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> new CriticalReception("s", "f", 2100, 4.0, 4.0, 0)) // score == scale
                .doesNotThrowAnyException();
        assertThat(new CriticalReception("s", "f", 2024, 0.0, 10.0, 0).hasCritics()).isFalse();
    }

    @Test
    void rejectsABlankOrNullSource() {
        assertThatThrownBy(() -> new CriticalReception("  ", "Cannes", 2024, 1, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalReception(null, "Cannes", 2024, 1, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankOrNullFestival() {
        assertThatThrownBy(() -> new CriticalReception("s", "  ", 2024, 1, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalReception("s", null, 2024, 1, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnOutOfRangeEditionYear() {
        assertThatThrownBy(() -> new CriticalReception("s", "f", 1899, 1, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2101, 1, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeScoreOrAScoreAboveTheScale() {
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, -0.1, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, 4.1, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAScaleThatIsZeroOrNegative() {
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, 0, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, 0, -4, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonFiniteScoreOrScale() {
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, Double.NaN, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, Double.POSITIVE_INFINITY, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, 1, Double.NaN, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, 1, Double.POSITIVE_INFINITY, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeCriticCount() {
        assertThatThrownBy(() -> new CriticalReception("s", "f", 2024, 1, 4, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
