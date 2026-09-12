package com.magomez.androidapps.movierec.scoring;

import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic, pure computation of a movie's {@link QualityScore} from its ratings.
 *
 * <p>Steps: drop low-confidence ratings (a known vote count below {@link #MIN_CONFIDENT_VOTES}),
 * require the survivors to actually corroborate each other, normalize them to the common
 * 0-100 scale with {@link RatingNormalizer}, take the <em>simple arithmetic mean</em> (no
 * per-source weighting yet), round to one decimal.
 *
 * <ul>
 *   <li>no ratings, or none that clear the vote-count floor &rarr; {@link QualityScore#noData()};</li>
 *   <li>a rating with an unknown ({@code null}) vote count is kept — sources like Rotten
 *       Tomatoes / Metacritic never report one, so we can't apply the same floor to it;</li>
 *   <li>exactly one confident rating <em>and</em> it is one of those unknown-vote-count
 *       ones &rarr; still {@link QualityScore#noData()}. A single number nobody can size
 *       ("96 on Rotten Tomatoes", no critic count — could be three glowing festival
 *       reviews) must not carry the same weight as a rating actually verified to have real
 *       volume behind it. A <em>second</em> independent source agreeing is what corroborates
 *       it, exactly like a lone but volume-verified TMDB/IMDb rating already does on its own;</li>
 *   <li>an unknown rating source &rarr; the {@link UnknownRatingSourceException} from the
 *       normalizer propagates (never ignored silently).</li>
 * </ul>
 *
 * <p>The vote-count floor keeps an unreleased film with a mere handful of early votes
 * (e.g. {@code vote_average 9.0} from 5 votes) from getting a real QUALITY SCORE. It is
 * deliberately low ({@link #MIN_CONFIDENT_VOTES}): many festival titles carry a small but
 * real vote base, and a film that never clears the floor still competes on affinity alone.
 *
 * <p>No LLM. No PERSONAL MATCH SCORE, no ranking. Original {@link Movie} and
 * {@link Rating}s are never modified.
 */
@Component
public class QualityScoreCalculator {

    /** A rating whose known vote count is below this is treated as too thin to trust. */
    static final int MIN_CONFIDENT_VOTES = 10;

    private final RatingNormalizer ratingNormalizer;

    public QualityScoreCalculator(RatingNormalizer ratingNormalizer) {
        this.ratingNormalizer = ratingNormalizer;
    }

    public QualityScore calculate(Movie movie) {
        Objects.requireNonNull(movie, "movie");

        List<Rating> confident = movie.ratings().stream()
                .filter(QualityScoreCalculator::isConfident)
                .toList();
        if (confident.isEmpty()) {
            return QualityScore.noData();
        }

        // Normalize (and so validate the source) before the corroboration check: an
        // unrecognised source must always throw, never be swallowed as "not corroborated".
        List<NormalizedRating> normalized = confident.stream()
                .map(ratingNormalizer::normalize) // may throw UnknownRatingSourceException
                .toList();
        if (!isCorroborated(confident)) {
            return QualityScore.noData();
        }

        double mean = normalized.stream()
                .mapToDouble(NormalizedRating::score)
                .average()
                .orElseThrow(); // confident is non-empty here

        return QualityScore.of(roundToOneDecimal(mean), normalized);
    }

    private static boolean isConfident(Rating rating) {
        Integer votes = rating.voteCount();
        return votes == null || votes >= MIN_CONFIDENT_VOTES;
    }

    /**
     * A single rating is trustworthy on its own only when we actually verified it has real
     * volume behind it (a known vote count that cleared the floor). A source that never
     * reports a vote count (Rotten Tomatoes, Metacritic) needs a second confident rating —
     * of either kind — to corroborate it; alone, it could be three reviews or three hundred
     * and we have no way to tell.
     */
    private static boolean isCorroborated(List<Rating> confident) {
        if (confident.isEmpty()) {
            return false;
        }
        if (confident.size() >= 2) {
            return true;
        }
        return confident.get(0).voteCount() != null;
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
