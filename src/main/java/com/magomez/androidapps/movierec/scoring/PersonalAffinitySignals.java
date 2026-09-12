package com.magomez.androidapps.movierec.scoring;

/**
 * The per-dimension personal-affinity signals computed for one movie against one
 * {@link UserTasteProfile}. Output of {@link PersonalAffinityCalculator}.
 *
 * <p>Three dimensions only — <b>director</b>, <b>genre</b>, <b>actor</b>. Country and
 * decade were removed from the PERSONAL MATCH SCORE: they are neither computed nor
 * returned nor allowed to influence the ranking.
 *
 * <p>Each signal is kept individually (not yet combined here) so a later phase can
 * explain <em>why</em> a movie fits a user. Every value is within {@code [0, 100]};
 * {@code 0} also means "not enough information".
 *
 * <p>No PERSONAL MATCH SCORE and no mixing with QUALITY SCORE here.
 */
public record PersonalAffinitySignals(
        double directorAffinity,
        double genreAffinity,
        double actorAffinity) {

    public PersonalAffinitySignals {
        directorAffinity = requireInRange(directorAffinity, "directorAffinity");
        genreAffinity = requireInRange(genreAffinity, "genreAffinity");
        actorAffinity = requireInRange(actorAffinity, "actorAffinity");
    }

    private static double requireInRange(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException(name + " must be within [0, 100]: " + value);
        }
        return value;
    }
}
