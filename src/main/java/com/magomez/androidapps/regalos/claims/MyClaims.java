package com.magomez.androidapps.regalos.claims;

import java.util.List;

/**
 * GET /me/claims. The totals only count the gifts I give myself: spent is what I bought,
 * pending what I reserved and the owner still wants.
 */
public record MyClaims(long spentCents, long pendingCents, int boughtCount, int totalCount, List<ClaimGroup> groups) {
}
