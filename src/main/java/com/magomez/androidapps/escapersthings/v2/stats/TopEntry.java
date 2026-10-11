package com.magomez.androidapps.escapersthings.v2.stats;

import java.math.BigDecimal;

/** A room in one of my tops, with my score for that top's criterion. */
public record TopEntry(int id, String name, BigDecimal score) {
}
