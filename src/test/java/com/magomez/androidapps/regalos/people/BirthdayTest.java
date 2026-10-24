package com.magomez.androidapps.regalos.people;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static org.assertj.core.api.Assertions.assertThat;

/** Day and month without a year, and how many days are left until the next one. */
class BirthdayTest {

    private static final LocalDate OCT_24_2026 = LocalDate.of(2026, 10, 24);

    @Test
    void todayIsZeroDaysAway() {
        assertThat(new Birthday(24, 10).daysUntil(OCT_24_2026)).isZero();
    }

    @Test
    void laterThisYearCountsTheDaysLeft() {
        assertThat(new Birthday(12, 11).daysUntil(OCT_24_2026)).isEqualTo(19);
    }

    @Test
    void alreadyPassedThisYearCountsUntilNextYear() {
        assertThat(new Birthday(23, 10).daysUntil(OCT_24_2026)).isEqualTo(364);
    }

    @Test
    void the29thOfFebruaryIsTheFirstOfMarchInACommonYear() {
        Birthday leapling = new Birthday(29, 2);

        assertThat(leapling.daysUntil(LocalDate.of(2027, 2, 28))).isEqualTo(1);
        assertThat(leapling.daysUntil(LocalDate.of(2027, 3, 1))).isZero();
        assertThat(leapling.daysUntil(LocalDate.of(2028, 2, 28))).isEqualTo(1);
    }

    @Test
    void validAcceptsRealDaysAndThe29thOfFebruary() {
        assertThat(Birthday.valid(new Birthday(29, 2))).isEqualTo(new Birthday(29, 2));
        assertThat(Birthday.valid(new Birthday(31, 12))).isEqualTo(new Birthday(31, 12));
        assertThat(Birthday.valid(null)).isNull();
    }

    @Test
    void validRejectsDaysThatNoMonthHas() {
        for (Birthday wrong : new Birthday[]{new Birthday(30, 2), new Birthday(31, 4), new Birthday(0, 5),
                new Birthday(12, 13), new Birthday(12, 0)}) {
            assertRegalosError(() -> Birthday.valid(wrong), HttpStatus.BAD_REQUEST, "bad_request");
        }
    }

    @Test
    void ofColumnsIsNullWhenTheUserHasNoBirthday() {
        assertThat(Birthday.ofColumns(null, null)).isNull();
        assertThat(Birthday.ofColumns(12, 11)).isEqualTo(new Birthday(12, 11));
    }
}
