package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.items.ItemStatus;
import com.magomez.androidapps.regalos.people.Birthday;

/** A wish with its owner and its claim; the claim columns are null when there is none. */
record ClaimRow(long id, int ownerId, String ownerName, Birthday ownerBirthday, String title, String url,
                String store, Integer priceCents, String imageUrl, String notes, boolean wantedALot,
                ItemStatus status, ClaimState claimState, Integer markedBy, Integer giverUserId, String giverName,
                boolean editedSinceClaim) {

    boolean claimed() {
        return claimState != null;
    }

    boolean givenBy(int userId) {
        return giverUserId != null && giverUserId == userId;
    }

    /** R4: whoever claimed it, or the user it was assigned to. */
    boolean editableBy(int userId) {
        return claimed() && ((markedBy != null && markedBy == userId) || givenBy(userId));
    }
}
