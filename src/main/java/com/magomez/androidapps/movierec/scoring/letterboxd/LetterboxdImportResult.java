package com.magomez.androidapps.movierec.scoring.letterboxd;

import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.scoring.RatedMovie;

import java.util.List;
import java.util.Objects;

/**
 * Result of trying to relate one Letterboxd CSV entry to one of our identified movies.
 * Immutable; no entry is ever dropped silently.
 *
 * <p>It keeps the original Letterboxd data ({@code letterboxdTitle}, {@code year},
 * {@code letterboxdUri}, {@code userScore}), the identification outcome
 * ({@code status}, plus {@code candidates} when {@code AMBIGUOUS}), and — only when
 * {@code IDENTIFIED} — the resulting {@link RatedMovie}.
 *
 * @param letterboxdTitle     the title exactly as it came from the CSV
 * @param year                the year from the CSV, or {@code null}
 * @param letterboxdUri       the URI exactly as it came from the CSV, or {@code null}
 * @param userScore           the Letterboxd rating on the 0-10 scale
 * @param status              identification outcome
 * @param candidates          competing matches, present only when {@code AMBIGUOUS}
 * @param ratedMovie          identified movie + userScore, present only when {@code IDENTIFIED}
 * @param identificationError provider failure message when identification could not be
 *                            attempted/completed, otherwise {@code null}
 */
public record LetterboxdImportResult(
        String letterboxdTitle,
        Integer year,
        String letterboxdUri,
        double userScore,
        IdentificationStatus status,
        List<String> candidates,
        RatedMovie ratedMovie,
        String identificationError) {

    public LetterboxdImportResult {
        Objects.requireNonNull(letterboxdTitle, "letterboxdTitle");
        Objects.requireNonNull(status, "status");
        candidates = candidates == null ? List.of() : List.copyOf(candidates);

        boolean identified = status == IdentificationStatus.IDENTIFIED;
        if (identified && ratedMovie == null) {
            throw new IllegalArgumentException("IDENTIFIED result must carry a RatedMovie");
        }
        if (!identified && ratedMovie != null) {
            throw new IllegalArgumentException("non-IDENTIFIED result must not carry a RatedMovie");
        }
    }

    public boolean isIdentified() {
        return status == IdentificationStatus.IDENTIFIED;
    }

    public static LetterboxdImportResult identified(LetterboxdRatedMovie entry, RatedMovie ratedMovie) {
        return of(entry, IdentificationStatus.IDENTIFIED, List.of(), ratedMovie, null);
    }

    public static LetterboxdImportResult notFound(LetterboxdRatedMovie entry) {
        return of(entry, IdentificationStatus.NOT_FOUND, List.of(), null, null);
    }

    public static LetterboxdImportResult ambiguous(LetterboxdRatedMovie entry, List<String> candidates) {
        return of(entry, IdentificationStatus.AMBIGUOUS, candidates, null, null);
    }

    public static LetterboxdImportResult identificationFailed(LetterboxdRatedMovie entry, String error) {
        return of(entry, IdentificationStatus.NOT_FOUND, List.of(), null, error);
    }

    private static LetterboxdImportResult of(LetterboxdRatedMovie entry, IdentificationStatus status,
                                             List<String> candidates, RatedMovie ratedMovie, String error) {
        return new LetterboxdImportResult(entry.title(), entry.year(), entry.letterboxdUri(),
                entry.userScore(), status, candidates, ratedMovie, error);
    }
}
