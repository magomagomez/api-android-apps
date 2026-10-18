package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.JcfitConfig;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** Weeks run Monday to Sunday; week 1 is the one that contains the program start. */
final class TrainingDates {

    static final int DAYS_PER_WEEK = 7;

    private TrainingDates() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /** The day in Spain, whatever the zone of the clock. */
    static LocalDate today(Clock clock) {
        return LocalDate.ofInstant(clock.instant(), JcfitConfig.SPAIN);
    }

    static LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 400 before_program_start for a week earlier than the start's. */
    static int weekNumber(LocalDate programStart, LocalDate date) {
        long weeks = ChronoUnit.WEEKS.between(monday(programStart), monday(date));
        if (weeks < 0) {
            throw TrainingErrors.beforeProgramStart();
        }
        return Math.toIntExact(weeks) + 1;
    }

    /** 400 when the date does not fall on the weekday of the program day. */
    static void requireDayOfWeek(int dayOfWeek, LocalDate date) {
        if (date.getDayOfWeek().getValue() != dayOfWeek) {
            throw TrainingErrors.dateNotOnDay();
        }
    }
}
