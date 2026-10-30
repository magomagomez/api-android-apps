package com.magomez.androidapps.regalos.claims;

import java.util.List;

/**
 * GET /me/claims. The totals count the gifts I give myself plus my bought ideas: spent is what
 * I bought, pending what I reserved and is still wanted. Ideas not bought do not count.
 */
public record MyClaims(long spentCents, long pendingCents, int boughtCount, int totalCount, List<ClaimGroup> groups) {
}
