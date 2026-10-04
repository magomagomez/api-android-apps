package com.magomez.androidapps.jctravels.rides.service;

import com.magomez.androidapps.jctravels.rides.dto.ParkFanAttraction;
import com.magomez.androidapps.jctravels.rides.dto.ParkFanSearch;
import com.magomez.androidapps.jctravels.rides.dto.RideForecastDTO;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** The pure part of the park.fan forecast: which result is our ride, and what the app gets. */
public final class RideForecasts {

    public static final String SOURCE = "park.fan";
    private static final String ATTRACTION = "attraction";

    private RideForecasts() {
    }

    /** The URL of the attraction with exactly this official name in this park, or null. */
    public static String match(ParkFanSearch search, String rideName, String parkName) {
        if (search == null || search.results() == null) {
            return null;
        }
        String ride = normalized(rideName);
        String park = normalized(parkName);
        return search.results().stream()
                .filter(result -> ATTRACTION.equals(result.type()) && result.parentPark() != null)
                .filter(result -> ride.equals(normalized(result.name())) && park.equals(normalized(result.parentPark().name())))
                .map(ParkFanSearch.Result::url)
                .findFirst()
                .orElse(null);
    }

    public static RideForecastDTO toDto(ParkFanAttraction attraction) {
        List<RideForecastDTO.Point> points = attraction.hourlyForecast() == null ? List.of() : attraction.hourlyForecast().stream()
                .filter(p -> p.predictedTime() != null && p.predictedWaitTime() != null)
                .sorted(Comparator.comparing(p -> OffsetDateTime.parse(p.predictedTime())))
                .map(p -> new RideForecastDTO.Point(p.predictedTime(), p.predictedWaitTime(), p.uncertaintyMinutes()))
                .toList();
        List<RideForecastDTO.TypicalDay> typical = attraction.typicalWaits() == null || attraction.typicalWaits().byDayOfWeek() == null
                ? List.of()
                : attraction.typicalWaits().byDayOfWeek().stream()
                        .filter(day -> day.dayOfWeek() != null && day.typical() != null)
                        .map(day -> new RideForecastDTO.TypicalDay(day.dayOfWeek(), day.typical(), day.busy()))
                        .toList();
        String timezone = attraction.park() == null ? null : attraction.park().timezone();
        return new RideForecastDTO(points, typical, timezone, SOURCE);
    }

    public static RideForecastDTO empty() {
        return new RideForecastDTO(List.of(), List.of(), null, SOURCE);
    }

    private static String normalized(String name) {
        return Objects.requireNonNullElse(name, "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
