package com.magomez.androidapps.jctravels.parks.service;

import com.magomez.androidapps.jctravels.parks.dto.ParkHoursDTO;
import com.magomez.androidapps.jctravels.parks.dto.ScheduleEntry;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/** Picks the opening the park header shows: today while it has not closed, else the next day it opens. */
public final class ParkHours {

    private static final String OPERATING = "OPERATING";
    private static final String TICKETED_EVENT = "TICKETED_EVENT";
    private static final String EARLY_ENTRY = "early entry";

    private ParkHours() {
    }

    public static ParkHoursDTO next(List<ScheduleEntry> schedule, Instant now) {
        return schedule.stream()
                .filter(entry -> OPERATING.equals(entry.type()) && entry.openingTime() != null && entry.closingTime() != null)
                .filter(entry -> instant(entry.closingTime()).isAfter(now))
                .min(Comparator.comparing(entry -> instant(entry.openingTime())))
                .map(day -> new ParkHoursDTO(day.date(), day.openingTime(), day.closingTime(), earlyEntry(schedule, day.date())))
                .orElse(null);
    }

    private static String earlyEntry(List<ScheduleEntry> schedule, String date) {
        return schedule.stream()
                .filter(entry -> TICKETED_EVENT.equals(entry.type()) && date.equals(entry.date()))
                .filter(entry -> entry.description() != null && entry.description().toLowerCase().contains(EARLY_ENTRY))
                .map(ScheduleEntry::openingTime)
                .findFirst()
                .orElse(null);
    }

    private static Instant instant(String time) {
        return OffsetDateTime.parse(time).toInstant();
    }
}
