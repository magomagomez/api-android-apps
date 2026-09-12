package com.magomez.androidapps.movierec.model;

/**
 * Result of trying to identify a movie against an external data source.
 *
 * <p>The identification is deterministic and explainable: an ambiguous title is
 * never promoted to {@link #IDENTIFIED} automatically.
 */
public enum IdentificationStatus {

    /** Exactly one movie could be resolved for the query. */
    IDENTIFIED,

    /** No movie matched the query. */
    NOT_FOUND,

    /** Several plausible movies matched and none could be selected deterministically. */
    AMBIGUOUS
}
