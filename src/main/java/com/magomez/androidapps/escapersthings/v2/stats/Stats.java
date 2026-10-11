package com.magomez.androidapps.escapersthings.v2.stats;

import java.math.BigDecimal;
import java.util.List;

/** GET /me/stats. myAverage and groupAverage are means of global scores, 2 decimals, or null. */
public record Stats(int total, BigDecimal myAverage, BigDecimal groupAverage, List<CategoryCount> byCategory,
                    Tops tops) {
}
