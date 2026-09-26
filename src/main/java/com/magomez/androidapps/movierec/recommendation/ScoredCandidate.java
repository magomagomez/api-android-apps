package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.model.FilmScreening;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;

import java.util.List;
import java.util.Objects;

/**
 * An identified, not-yet-seen candidate together with the scoring the current algorithm
 * produced for it against the user's {@link com.magomez.androidapps.movierec.scoring.UserTasteProfile}.
 *
 * <p>Temporary and immutable.
 *
 * @param title            the title exactly as it was requested
 * @param movie            the identified + enriched movie
 * @param score            the {@link PersonalMatchScore} from {@code PersonalMatchScoreCalculator}
 * @param similaritySignal how strongly the candidate resembles films the user has seen
 *                         (experimental, observation only — never used for scoring/ranking)
 * @param accoladeSignal   how much external recognition (awards / festivals) the candidate
 *                         carries (observation only for now — never used for scoring/ranking)
 * @param reason           the deterministic explanation of the recommendation
 * @param enrichmentError  provider failure message when enrichment (e.g. OMDb) partially
 *                         failed, otherwise {@code null}; identification still succeeded
 * @param screenings       when and where the festival actually screens this film — empty
 *                         when it isn't in the programme (never guessed; see
 *                         {@code ScheduleService.screeningsOf})
 */
public record ScoredCandidate(
        String title,
        Movie movie,
        PersonalMatchScore score,
        SimilaritySignal similaritySignal,
        AccoladeSignal accoladeSignal,
        RecommendationReason reason,
        String enrichmentError,
        List<FilmScreening> screenings) {

    public ScoredCandidate {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(movie, "movie");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(similaritySignal, "similaritySignal");
        Objects.requireNonNull(accoladeSignal, "accoladeSignal");
        Objects.requireNonNull(reason, "reason");
        screenings = screenings == null ? List.of() : List.copyOf(screenings);
    }

    /** Backward-compatible constructor without schedule screenings (defaults to none). */
    public ScoredCandidate(String title, Movie movie, PersonalMatchScore score,
                           SimilaritySignal similaritySignal, AccoladeSignal accoladeSignal,
                           RecommendationReason reason, String enrichmentError) {
        this(title, movie, score, similaritySignal, accoladeSignal, reason, enrichmentError, List.of());
    }

    /** Backward-compatible constructor without the accolade signal (defaults to none). */
    public ScoredCandidate(String title, Movie movie, PersonalMatchScore score,
                           SimilaritySignal similaritySignal, RecommendationReason reason,
                           String enrichmentError) {
        this(title, movie, score, similaritySignal, AccoladeSignal.none(), reason, enrichmentError, List.of());
    }

    /** Backward-compatible constructor without the similarity / accolade signals. */
    public ScoredCandidate(String title, Movie movie, PersonalMatchScore score,
                           RecommendationReason reason, String enrichmentError) {
        this(title, movie, score, SimilaritySignal.none(), AccoladeSignal.none(), reason, enrichmentError, List.of());
    }

    /** {@code true} when the festival programme places at least one convenient screening for this film. */
    public boolean hasConvenientScreening() {
        return screenings.stream().anyMatch(s ->
                com.magomez.androidapps.movierec.schedule.SchedulePriority.isConvenient(s.date(), s.startTime()));
    }
}
