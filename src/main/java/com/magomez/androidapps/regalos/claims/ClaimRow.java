package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.items.ItemStatus;
import com.magomez.androidapps.regalos.people.Birthday;

import java.time.Instant;

/**
 * A wish with its owner (a person, or a shared list) and its claim; the claim columns are null
 * when there is none.
 */
record ClaimRow(long id, Integer ownerId, String ownerName, Birthday ownerBirthday, Long listId, String listName,
                Birthday listBirthday, String addedByName, String title, String url, String store, Integer priceCents,
                String imageUrl, String notes, boolean wantedALot, ItemStatus status, ClaimState claimState,
                Integer markedBy, Integer giverUserId, String giverName, boolean editedSinceClaim, Instant photoVersion) {

    boolean claimed() {
        return claimState != null;
    }

    boolean ownedBy(int userId) {
        return ownerId != null && ownerId == userId;
    }

    boolean inList() {
        return listId != null;
    }

    boolean givenBy(int userId) {
        return giverUserId != null && giverUserId == userId;
    }

    /** R4: whoever claimed it, or the user it was assigned to. */
    boolean editableBy(int userId) {
        return claimed() && ((markedBy != null && markedBy == userId) || givenBy(userId));
    }
}
