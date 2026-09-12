package com.magomez.androidapps.movierec.model;

/**
 * A single critical-reception observation for a movie, as reported by one concrete
 * source at one festival edition (e.g. a trade's critics' grid, a jury grid).
 *
 * <p>Domain value object: immutable, temporary, no transport / provider detail. The
 * values are kept exactly as the source reported them — <b>no normalization here</b>
 * (the {@code score}/{@code scale} pair says which scale they are on).
 *
 * @param source      the concrete source, e.g. {@code "Screen Jury Grid"} (never blank)
 * @param festival    festival name, e.g. {@code "Cannes"} (never blank)
 * @param editionYear the edition's year (1900-2100)
 * @param score       the value reported by the source ({@code >= 0}, {@code <= scale})
 * @param scale       the maximum of the source's own scale ({@code > 0})
 * @param criticCount how many critics/jurors backed the score ({@code >= 0})
 */
public record CriticalReception(
        String source,
        String festival,
        int editionYear,
        double score,
        double scale,
        int criticCount) {

    private static final int MIN_EDITION_YEAR = 1900;
    private static final int MAX_EDITION_YEAR = 2100;

    public CriticalReception {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        source = source.trim();

        if (festival == null || festival.isBlank()) {
            throw new IllegalArgumentException("festival must not be blank");
        }
        festival = festival.trim();

        if (editionYear < MIN_EDITION_YEAR || editionYear > MAX_EDITION_YEAR) {
            throw new IllegalArgumentException("editionYear out of range: " + editionYear);
        }

        if (!Double.isFinite(score) || score < 0.0) {
            throw new IllegalArgumentException("score must be a finite value >= 0: " + score);
        }
        if (!Double.isFinite(scale) || scale <= 0.0) {
            throw new IllegalArgumentException("scale must be a finite value > 0: " + scale);
        }
        if (score > scale) {
            throw new IllegalArgumentException(
                    "score (" + score + ") must not exceed scale (" + scale + ")");
        }
        if (criticCount < 0) {
            throw new IllegalArgumentException("criticCount must not be negative: " + criticCount);
        }
    }

    public static CriticalReception of(String source, String festival, int editionYear,
                                       double score, double scale, int criticCount) {
        return new CriticalReception(source, festival, editionYear, score, scale, criticCount);
    }

    /** Whether the source reported at least one backing critic/juror. */
    public boolean hasCritics() {
        return criticCount > 0;
    }
}
