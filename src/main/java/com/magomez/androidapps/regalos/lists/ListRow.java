package com.magomez.androidapps.regalos.lists;

import com.magomez.androidapps.regalos.people.Birthday;

/** A regalos_lists row with its counts for the caller, before the days until its birthday. */
record ListRow(long id, String name, Birthday birthday, int createdById, String createdByName, int memberCount,
               int wishCount, int freeCount, int mineCount) {
}
