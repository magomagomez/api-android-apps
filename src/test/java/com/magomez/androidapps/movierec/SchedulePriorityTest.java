package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.schedule.SchedulePriority;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SchedulePriority}: a screening is "convenient" only on a realistic personal
 * schedule — weekday evenings, or a day that's free all day (weekend, or the 12th, this
 * festival's fixed public holiday).
 */
class SchedulePriorityTest {

    @Test
    void aWeekdayMorningOrAfternoonIsNotConvenient() {
        LocalDate wednesday = LocalDate.of(2026, 10, 14); // Wed
        assertThat(SchedulePriority.isConvenient(wednesday, LocalTime.of(9, 0))).isFalse();
        assertThat(SchedulePriority.isConvenient(wednesday, LocalTime.of(17, 59))).isFalse();
    }

    @Test
    void aWeekdayEveningFrom6pmIsConvenient() {
        LocalDate wednesday = LocalDate.of(2026, 10, 14);
        assertThat(SchedulePriority.isConvenient(wednesday, LocalTime.of(18, 0))).isTrue();
        assertThat(SchedulePriority.isConvenient(wednesday, LocalTime.of(23, 0))).isTrue();
    }

    @Test
    void aWeekendDayIsConvenientAtAnyTime() {
        LocalDate saturday = LocalDate.of(2026, 10, 10);
        LocalDate sunday = LocalDate.of(2026, 10, 11);
        assertThat(SchedulePriority.isConvenient(saturday, LocalTime.of(9, 0))).isTrue();
        assertThat(SchedulePriority.isConvenient(sunday, LocalTime.of(12, 0))).isTrue();
    }

    @Test
    void theTwelfthIsConvenientAtAnyTimeEvenThoughItsAMonday() {
        LocalDate theTwelfth = LocalDate.of(2026, 10, 12);
        assertThat(theTwelfth.getDayOfWeek().toString()).isEqualTo("MONDAY");
        assertThat(SchedulePriority.isConvenient(theTwelfth, LocalTime.of(9, 0))).isTrue();
    }

    @Test
    void theTwelfthOfAnyOtherMonthIsAlsoTreatedAsTheFixedHoliday() {
        // The rule is "day-of-month == 12" on purpose: Spain's 12 October holiday is fixed
        // every year, so this generalises across editions without hardcoding the year.
        assertThat(SchedulePriority.isConvenient(LocalDate.of(2027, 3, 12), LocalTime.of(9, 0))).isTrue();
    }

    @Test
    void aNullTimeOnAWeekdayIsNotConvenient() {
        assertThat(SchedulePriority.isConvenient(LocalDate.of(2026, 10, 14), null)).isFalse();
    }
}
