package com.magomez.androidapps.escapersthings.v2.stats;

import com.magomez.androidapps.escapersthings.v2.rooms.Grade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Tops of my grades, ordered like v1's /user/{id}/grades so both agree: the criterion, then
 * a second criterion as Postgres sorts it (NULL counts as the largest value), then the name.
 */
final class StatsCalculator {

    static final int TOP_SIZE = 5;
    private static final int AVERAGE_SCALE = 2;

    private StatsCalculator() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static Tops tops(List<MyGrade> grades) {
        return new Tops(
                top(grades, Grade::global, Grade::terror, true),
                top(grades, Grade::terror, Grade::global, true),
                top(grades, Grade::immersion, Grade::global, true),
                top(grades, Grade::gameMaster, Grade::global, true),
                top(grades, Grade::global, Grade::terror, false));
    }

    /** Mean of my global scores, or null when I have none. */
    static BigDecimal myAverage(List<MyGrade> grades) {
        List<BigDecimal> globals = grades.stream().map(g -> g.grade().global()).filter(Objects::nonNull).toList();
        if (globals.isEmpty()) {
            return null;
        }
        return round(globals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(globals.size()), AVERAGE_SCALE, RoundingMode.HALF_UP));
    }

    static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    private static List<TopEntry> top(List<MyGrade> grades, Function<Grade, BigDecimal> criterion,
                                      Function<Grade, BigDecimal> tieBreak, boolean highestFirst) {
        Comparator<BigDecimal> nullsLargest = Comparator.nullsLast(Comparator.naturalOrder());
        Comparator<MyGrade> order = Comparator.comparing((MyGrade g) -> criterion.apply(g.grade()), nullsLargest)
                .thenComparing(g -> tieBreak.apply(g.grade()), nullsLargest);
        if (highestFirst) {
            order = order.reversed();
        }
        return grades.stream()
                .filter(g -> criterion.apply(g.grade()) != null)
                .sorted(order.thenComparing(MyGrade::roomName).thenComparingInt(MyGrade::roomId))
                .limit(TOP_SIZE)
                .map(g -> new TopEntry(g.roomId(), g.roomName(), criterion.apply(g.grade())))
                .toList();
    }
}
