package com.magomez.androidapps.movierec.api;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.recommendation.AccoladeSignal;
import com.magomez.androidapps.movierec.recommendation.ExcludedCandidate;
import com.magomez.androidapps.movierec.recommendation.ExclusionReason;
import com.magomez.androidapps.movierec.recommendation.RecommendationResult;
import com.magomez.androidapps.movierec.recommendation.ScoredCandidate;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates between the HTTP contract ({@link RecommendationRequest} /
 * {@link RecommendationResponse}) and the domain ({@link MovieQuery} /
 * {@link RecommendationResult}).
 *
 * <p>Pure and stateless, like {@link MovieApiMapper}. It is the single place that knows
 * both the JSON shape and the recommendation domain, so neither the service nor the
 * scoring code has to.
 */
public final class RecommendationApiMapper {

    /** Native scale of each rating source we currently know, for display only. */
    private static final Map<String, Integer> RATING_SCALE = Map.of(
            "TMDB", 10, "IMDb", 10, "Rotten Tomatoes", 100, "Metacritic", 100);

    private RecommendationApiMapper() {
    }

    /** Validates the request and converts it to domain candidate queries. */
    public static List<MovieQuery> toQueries(RecommendationRequest request) {
        List<RecommendationRequest.Entry> entries = request == null ? List.of() : request.movies();

        List<MovieQuery> queries = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            RecommendationRequest.Entry entry = entries.get(i);
            if (entry == null || entry.title() == null || entry.title().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "movies[" + i + "].title is required");
            }
            queries.add(new MovieQuery(entry.title(), entry.year(), entry.director()));
        }
        return queries;
    }

    public static RecommendationResponse toResponse(RecommendationResult result) {
        List<RecommendationResponse.RecommendationItem> recommendations =
                new ArrayList<>(result.recommendations().size());
        int position = 1;
        int withQuality = 0;
        int withCompletePms = 0;
        for (ScoredCandidate candidate : result.recommendations()) {
            PersonalMatchScore score = candidate.score();
            if (score.qualityScore().hasValue()) {
                withQuality++;
            }
            if (score.isComplete()) {
                withCompletePms++;
            }
            recommendations.add(toRecommendationItem(position++, candidate));
        }

        List<RecommendationResponse.ExcludedItem> excluded = result.excluded().stream()
                .map(RecommendationApiMapper::toExcludedItem)
                .toList();

        RecommendationResponse.Summary summary = new RecommendationResponse.Summary(
                result.totalCandidates(),
                result.recommendations().size()
                        + (int) result.excludedFor(ExclusionReason.ALREADY_WATCHED)
                        + (int) result.excludedFor(ExclusionReason.DUPLICATE_CANDIDATE),
                (int) result.excludedFor(ExclusionReason.NOT_FOUND),
                (int) result.excludedFor(ExclusionReason.AMBIGUOUS),
                (int) result.excludedFor(ExclusionReason.ALREADY_WATCHED),
                (int) result.excludedFor(ExclusionReason.DUPLICATE_CANDIDATE),
                result.recommendations().size(),
                withQuality,
                withCompletePms);

        return new RecommendationResponse(summary, recommendations, excluded);
    }

    private static RecommendationResponse.RecommendationItem toRecommendationItem(
            int position, ScoredCandidate candidate) {
        Movie movie = candidate.movie();
        PersonalMatchScore score = candidate.score();
        AccoladeSignal accoladeSignal = candidate.accoladeSignal();

        return new RecommendationResponse.RecommendationItem(
                position,
                candidate.title(),
                movie.tmdbId(),
                movie.imdbId(),
                emptyToNull(movie.poster()),
                releaseYear(movie),
                emptyToNull(movie.overview()),
                movie.director() == null ? null : movie.director().name(),
                movie.actors().stream().map(Actor::name).toList(),
                movie.genres().stream().map(Genre::name).toList(),
                movie.ratings().stream().map(RecommendationApiMapper::toRatingView).toList(),
                score.estimatedValue().isPresent() ? score.estimatedValue().getAsDouble() : null,
                score.isComplete(),
                accoladeSignal.achievements().stream()
                        .map(a -> new RecommendationResponse.FestivalAchievementView(
                                a.festival(), a.editionYear(), a.section(),
                                a.type().name(), a.awardName()))
                        .toList(),
                accoladeSignal.highlights(),
                candidate.reason().reasons(),
                candidate.reason().text(),
                candidate.enrichmentError() == null
                        ? null
                        : "partial enrichment failure: " + candidate.enrichmentError());
    }

    private static RecommendationResponse.RatingView toRatingView(Rating rating) {
        return new RecommendationResponse.RatingView(
                rating.source(), rating.score(), RATING_SCALE.get(rating.source()), rating.voteCount());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static RecommendationResponse.ExcludedItem toExcludedItem(ExcludedCandidate excluded) {
        return new RecommendationResponse.ExcludedItem(
                excluded.title(),
                excluded.tmdbId(),
                excluded.reason(),
                excluded.candidates(),
                excluded.note());
    }

    private static Integer releaseYear(Movie movie) {
        String releaseDate = movie.releaseDate();
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
