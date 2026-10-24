package com.magomez.androidapps.regalos.people;

import com.magomez.androidapps.regalos.config.RegalosException;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.temporal.ChronoUnit;

/** A birthday without a year: the app only asks for day and month. */
public record Birthday(int day, int month) {

    private static final int FEBRUARY_29 = 29;
    private static final String INVALID = "El cumpleaños no es una fecha válida";

    /** The same value when it is a real day of some year (the 29th of February included); null stays null. */
    public static Birthday valid(Birthday birthday) {
        if (birthday == null) {
            return null;
        }
        if (birthday.month < Month.JANUARY.getValue() || birthday.month > Month.DECEMBER.getValue()
                || birthday.day < 1 || birthday.day > Month.of(birthday.month).maxLength()) {
            throw RegalosException.badRequest(INVALID);
        }
        return birthday;
    }

    /** From the two nullable columns of regalos_users (both null or both set). */
    public static Birthday ofColumns(Integer day, Integer month) {
        return day == null || month == null ? null : new Birthday(day, month);
    }

    /** 0 on the day itself; the 29th of February falls on the 1st of March in a common year. */
    public int daysUntil(LocalDate today) {
        LocalDate next = inYear(today.getYear());
        if (next.isBefore(today)) {
            next = inYear(today.getYear() + 1);
        }
        return (int) ChronoUnit.DAYS.between(today, next);
    }

    private LocalDate inYear(int year) {
        if (month == Month.FEBRUARY.getValue() && day == FEBRUARY_29 && !Year.isLeap(year)) {
            return LocalDate.of(year, Month.MARCH, 1);
        }
        return LocalDate.of(year, month, day);
    }
}
