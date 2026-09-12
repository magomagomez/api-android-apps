package com.magomez.androidapps.movierec.recommendation;

import java.util.List;
import java.util.Objects;

/**
 * One movie in the user's Letterboxd history that a candidate is genuinely similar to —
 * i.e. a TMDB "similar" connection that passed the strong filter (<b>same director</b>,
 * or <b>&ge; 2 shared genres</b>).
 *
 * <p>"Similarity" here means "this candidate resembles a film you have seen", built only
 * from the two films' own metadata (director, genres). It deliberately says nothing about
 * whether the user <em>likes</em> those genres — that is {@code genreAffinity}, a
 * different signal.
 *
 * @param watchedTitle     the watched film's title
 * @param watchedTmdbId    its TMDB id
 * @param watchedUserScore the user's Letterboxd rating for it, 0-10
 * @param sameDirector     whether the candidate and this film share a director
 * @param sharedGenres     the genres both films carry (readable names; empty when the
 *                         match is by director only)
 * @param strength         this single match's strength in {@code [0, 1]}
 * @param source           {@code SAME_DIRECTOR} or {@code SHARED_GENRES}
 */
public record SimilarityMatch(
        String watchedTitle,
        Integer watchedTmdbId,
        double watchedUserScore,
        boolean sameDirector,
        List<String> sharedGenres,
        double strength,
        Source source) {

    public enum Source { SAME_DIRECTOR, SHARED_GENRES }

    public SimilarityMatch {
        Objects.requireNonNull(source, "source");
        sharedGenres = sharedGenres == null ? List.of() : List.copyOf(sharedGenres);
        if (!Double.isFinite(strength) || strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("match strength must be within [0, 1]: " + strength);
        }
    }
}
