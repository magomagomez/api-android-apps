package com.magomez.androidapps.escapersthings.v2.rooms;

import java.math.BigDecimal;

/** Group mean per criterion over the scores that are not null, 2 decimals, or null. */
public record Averages(BigDecimal enigma, BigDecimal immersion, BigDecimal terror, BigDecimal gameMaster,
                       BigDecimal global) {
}
