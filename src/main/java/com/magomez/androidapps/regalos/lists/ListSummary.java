package com.magomez.androidapps.regalos.lists;

import com.magomez.androidapps.regalos.people.Birthday;

/**
 * A list for someone outside the app, as the mailbox shows it: its ACTIVE wishes, how many
 * nobody claimed and how many I give. canManage: I created it.
 */
public record ListSummary(long id, String name, Birthday birthday, Integer daysUntilBirthday, ListCreator createdBy,
                          int memberCount, int wishCount, int freeCount, int mineCount, boolean canManage) {
}
