package com.magomez.androidapps.regalos.people;

/**
 * An element of GET /people: the person plus their ACTIVE wishes, how many nobody has
 * claimed yet, how many the caller gives and how many ideas the caller noted for them.
 */
public record PersonSummary(int id, String displayName, Birthday birthday, Integer daysUntilBirthday,
                            int wishCount, int freeCount, int mineCount, int ideaCount) {
}
