package com.magomez.androidapps.escapersthings.v2.stats;

import java.util.List;

/** Five rooms each, by my own grade: best and worst by global, the rest by their criterion. */
public record Tops(List<TopEntry> best, List<TopEntry> terror, List<TopEntry> immersion, List<TopEntry> gameMaster,
                   List<TopEntry> worst) {
}
