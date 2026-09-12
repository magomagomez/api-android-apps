package com.magomez.androidapps.movierec.recommendation;

/**
 * Why a candidate movie did not make it into the recommendation ranking.
 *
 * <p>Deterministic and explainable: every excluded candidate carries exactly one of
 * these, so an experiment can see what happened to each entry it sent.
 */
public enum ExclusionReason {

    /** The candidate could not be matched to any TMDB movie. */
    NOT_FOUND,

    /** Several plausible TMDB movies matched and none could be chosen deterministically. */
    AMBIGUOUS,

    /** The candidate resolves to a TMDB id already present in the user's Letterboxd library. */
    ALREADY_WATCHED,

    /** Another candidate in the same request already resolved to this TMDB id. */
    DUPLICATE_CANDIDATE
}
