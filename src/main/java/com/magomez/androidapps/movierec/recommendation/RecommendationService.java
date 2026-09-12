package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieIdentificationResult;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.provider.AggregatingAccoladeProvider;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScoreCalculator;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibrary;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibraryLoader;
import com.magomez.androidapps.movierec.service.MovieImportService;
import com.magomez.androidapps.movierec.support.ExternalCallExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The RECOMMENDATIONS pipeline: candidate movies in, ranked recommendations out.
 *
 * <pre>
 * candidates
 *   → TMDB identification + enrichment (reused MovieImportService)
 *   → exclude NOT_FOUND / AMBIGUOUS
 *   → exclude candidates already in the Letterboxd library (by TMDB id, never by title)
 *   → collapse duplicate candidates that resolve to the same TMDB id
 *   → score with the current algorithm (QualityScore + PersonalAffinity + PersonalMatchScore)
 *   → rank by PersonalMatchScore.estimatedValue descending, in one list
 * </pre>
 *
 * <p>This is <b>not</b> a movie-import endpoint: it only identifies candidates so it can
 * score them. It reuses the existing identification, enrichment and scoring untouched —
 * no formulas, weights, normalization, affinities or thresholds are changed here. No LLM,
 * no scraping, no TMDB discovery, no persistence: the Letterboxd library and every
 * intermediate object live only for the duration of the call.
 *
 * <p>Every scored candidate lands in {@link RecommendationResult#recommendations()} —
 * there is no separate "affinity-only" list. A candidate without a trustworthy QUALITY
 * still ranks, by {@code estimatedValue} (see {@link com.magomez.androidapps.movierec.scoring.PersonalMatchScore}),
 * instead of being hidden below every quality-backed film. {@code ranked()} /
 * {@code affinityOnly()} on the result are just filtered views for whoever wants the
 * distinction.
 */
@Service
public class RecommendationService {

    private static final Comparator<ScoredCandidate> BY_TITLE_THEN_ID =
            Comparator.<ScoredCandidate, String>comparing(ScoredCandidate::title,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(c -> c.movie().tmdbId(), Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * Highest {@code estimatedValue} first (comparable across quality-backed and
     * affinity-only candidates alike — see {@code PersonalMatchScore.estimatedValue()}).
     * On a tie, a confirmed PERSONAL MATCH SCORE outranks a provisional estimate, then
     * higher quality wins, then title/id for full determinism.
     */
    private static final Comparator<ScoredCandidate> BY_ESTIMATED_VALUE =
            Comparator.<ScoredCandidate>comparingDouble(
                            c -> c.score().estimatedValue().orElse(Double.NEGATIVE_INFINITY))
                    .reversed()
                    .thenComparing(c -> c.score().isComplete() ? 1 : 0, Comparator.reverseOrder())
                    .thenComparing(c -> c.score().qualityScore().value().orElse(Double.NEGATIVE_INFINITY),
                            Comparator.reverseOrder())
                    .thenComparing(BY_TITLE_THEN_ID);

    private final MovieImportService movieImportService;
    private final LetterboxdLibraryLoader letterboxdLibraryLoader;
    private final PersonalMatchScoreCalculator personalMatchScoreCalculator;
    private final SimilaritySignalCalculator similaritySignalCalculator;
    private final AggregatingAccoladeProvider accoladeProvider;
    private final AccoladeSignalCalculator accoladeSignalCalculator;
    private final RecommendationReasoner recommendationReasoner;
    private final ExternalCallExecutor externalCallExecutor;

    public RecommendationService(MovieImportService movieImportService,
                                 LetterboxdLibraryLoader letterboxdLibraryLoader,
                                 PersonalMatchScoreCalculator personalMatchScoreCalculator,
                                 SimilaritySignalCalculator similaritySignalCalculator,
                                 AggregatingAccoladeProvider accoladeProvider,
                                 AccoladeSignalCalculator accoladeSignalCalculator,
                                 RecommendationReasoner recommendationReasoner,
                                 ExternalCallExecutor externalCallExecutor) {
        this.movieImportService = movieImportService;
        this.letterboxdLibraryLoader = letterboxdLibraryLoader;
        this.personalMatchScoreCalculator = personalMatchScoreCalculator;
        this.similaritySignalCalculator = similaritySignalCalculator;
        this.accoladeProvider = accoladeProvider;
        this.accoladeSignalCalculator = accoladeSignalCalculator;
        this.recommendationReasoner = recommendationReasoner;
        this.externalCallExecutor = externalCallExecutor;
    }

    /**
     * Non-blocking: {@code true} once the Letterboxd library is warm and a call to
     * {@link #recommend(List)} won't have to build it first. Lets the controller answer
     * instantly instead of blocking a request for however long the warm-up has left —
     * important on platforms with a hard request timeout (e.g. Heroku's 30s router limit).
     */
    public boolean isReady() {
        return letterboxdLibraryLoader.isReady();
    }

    public RecommendationResult recommend(List<MovieQuery> candidates) {
        Objects.requireNonNull(candidates, "candidates");

        LetterboxdLibrary library = loadLibrary();
        UserTasteProfile profile = library.profile();

        List<MovieIdentificationResult> identified = movieImportService.identifyAll(candidates);

        // Sequential: exclusions are cheap (no network) and duplicate-detection depends
        // on processing order.
        List<MovieIdentificationResult> eligible = new ArrayList<>();
        List<ExcludedCandidate> excluded = new ArrayList<>();
        Set<Integer> candidateIdsSeen = new HashSet<>();

        for (MovieIdentificationResult result : identified) {
            String title = result.query().title();
            IdentificationStatus status = result.match().status();

            if (status == IdentificationStatus.NOT_FOUND) {
                excluded.add(ExcludedCandidate.notFound(title, result.identificationError()));
                continue;
            }
            if (status == IdentificationStatus.AMBIGUOUS) {
                excluded.add(ExcludedCandidate.ambiguous(title, result.match().candidates()));
                continue;
            }

            Integer tmdbId = result.match().movie().tmdbId();
            if (library.hasWatched(tmdbId)) {
                excluded.add(ExcludedCandidate.alreadyWatched(title, tmdbId));
                continue;
            }
            if (tmdbId != null && !candidateIdsSeen.add(tmdbId)) {
                excluded.add(ExcludedCandidate.duplicate(title, tmdbId));
                continue;
            }

            eligible.add(result);
        }

        // Parallel: scoring fans out to external providers per candidate (Wikidata,
        // festival lineups, OMDb) — sequentially this alone can take longer than Heroku's
        // 30s router timeout once there are 80+ eligible candidates.
        List<ScoredCandidate> recommendations = new ArrayList<>(
                externalCallExecutor.map(eligible, result -> score(result, profile, library)));

        recommendations.sort(BY_ESTIMATED_VALUE);
        return new RecommendationResult(candidates.size(), recommendations, excluded, profile.patterns());
    }

    private ScoredCandidate score(MovieIdentificationResult result, UserTasteProfile profile,
                                  LetterboxdLibrary library) {
        Movie movie = result.match().movie();
        AccoladeSignal accoladeSignal = accoladeSignalCalculator.calculate(accoladeProvider.accoladesOf(movie));
        PersonalMatchScore score = personalMatchScoreCalculator.calculate(
                movie, profile, accoladeSignal.strength());
        SimilaritySignal similaritySignal = similaritySignalCalculator.calculate(movie, library);
        RecommendationReason reason = recommendationReasoner.explain(
                movie, profile, score, similaritySignal, library);
        return new ScoredCandidate(result.query().title(), movie, score, similaritySignal,
                accoladeSignal, reason, result.enrichmentError());
    }

    private LetterboxdLibrary loadLibrary() {
        try {
            return letterboxdLibraryLoader.load();
        } catch (IOException e) {
            throw new IllegalStateException("could not read the Letterboxd library", e);
        }
    }
}
