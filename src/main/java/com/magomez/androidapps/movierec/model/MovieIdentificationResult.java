package com.magomez.androidapps.movierec.model;

/**
 * Domain outcome of processing one {@link MovieQuery}: the identification result and,
 * for identified movies, the outcome of the rating-enrichment step.
 *
 * <p>Temporary and immutable. It knows nothing about HTTP or serialization.
 *
 * <p>The two error fields are kept separate on purpose: a movie can be identified
 * (usable {@code match}) and still have a failed enrichment.
 *
 * @param query               the query that was resolved
 * @param match               the identification outcome; when identified, its
 *                            {@code movie} already carries any rating that was fetched
 * @param identificationError data-source failure message when the movie could not be
 *                            identified at all, otherwise {@code null}
 * @param enrichmentError     data-source failure message when the movie was identified
 *                            but its rating could not be fetched, otherwise {@code null}
 */
public record MovieIdentificationResult(
        MovieQuery query,
        MovieMatch match,
        String identificationError,
        String enrichmentError) {

    public static MovieIdentificationResult of(MovieQuery query, MovieMatch match) {
        return new MovieIdentificationResult(query, match, null, null);
    }

    public static MovieIdentificationResult identificationFailed(MovieQuery query, String error) {
        return new MovieIdentificationResult(query, MovieMatch.notFound(), error, null);
    }

    /** Movie identified but its rating could not be fetched; the identified match is kept. */
    public static MovieIdentificationResult enrichmentFailed(
            MovieQuery query, MovieMatch identifiedMatch, String error) {
        return new MovieIdentificationResult(query, identifiedMatch, null, error);
    }
}
