package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieIdentificationResult;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.MovieDataProvider;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.recommendation.ExclusionReason;
import com.magomez.androidapps.movierec.recommendation.RecommendationReasoner;
import com.magomez.androidapps.movierec.recommendation.RecommendationResult;
import com.magomez.androidapps.movierec.recommendation.RecommendationService;
import com.magomez.androidapps.movierec.recommendation.ScoredCandidate;
import com.magomez.androidapps.movierec.scoring.PersonalAffinityCalculator;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScoreCalculator;
import com.magomez.androidapps.movierec.scoring.QualityScoreCalculator;
import com.magomez.androidapps.movierec.scoring.RatingNormalizer;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibrary;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibraryLoader;
import com.magomez.androidapps.movierec.service.MovieImportService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test of the RECOMMENDATIONS pipeline. Uses hand-written doubles for the reused
 * {@link MovieImportService} (candidate identification + enrichment) and
 * {@link LetterboxdLibraryLoader} (the user's library), with the <em>real</em>
 * {@link PersonalMatchScoreCalculator}. No TMDB, no OMDb, no Spring context.
 */
class RecommendationServiceTest {

    private final PersonalMatchScoreCalculator scoreCalculator = new PersonalMatchScoreCalculator(
            new QualityScoreCalculator(new RatingNormalizer()),
            new PersonalAffinityCalculator());

    /** A taste profile that prefers everything the "fav" movies below carry. */
    private final UserTasteProfile profile = new UserTasteProfile(
            Set.of("Fav Director"), Set.of("Drama"), Set.of("Fav Actor"),
            Set.of("France"), Set.of(2020));

    // --- doubles -----------------------------------------------------------------

    private static final MovieDataProvider UNUSED_PROVIDER = new MovieDataProvider() {
        @Override
        public String sourceName() {
            return "STUB";
        }

        @Override
        public MovieMatch identify(MovieQuery query) throws MovieProviderException {
            throw new MovieProviderException("not used");
        }
    };

    private RecommendationService serviceWith(
            LetterboxdLibrary library,
            Function<List<MovieQuery>, List<MovieIdentificationResult>> identify) {

        MovieImportService importStub = new MovieImportService(UNUSED_PROVIDER, List.of()) {
            @Override
            public List<MovieIdentificationResult> identifyAll(List<MovieQuery> queries) {
                return identify.apply(queries);
            }
        };
        LetterboxdLibraryLoader loaderStub = new LetterboxdLibraryLoader(null, null, "unused") {
            @Override
            public LetterboxdLibrary load() {
                return library;
            }
        };
        return new RecommendationService(importStub, loaderStub, scoreCalculator,
                new com.magomez.androidapps.movierec.recommendation.SimilaritySignalCalculator(),
                new com.magomez.androidapps.movierec.provider.AggregatingAccoladeProvider(List.of()),
                new com.magomez.androidapps.movierec.recommendation.AccoladeSignalCalculator(),
                new RecommendationReasoner());
    }

    private static Movie movie(int tmdbId, String title, String director, String genre,
                               String actor, String country, String releaseDate, double tmdbScore) {
        return new Movie(tmdbId, "tt" + tmdbId, title, title, releaseDate, 120, "overview",
                List.of(Genre.of(genre)), Director.of(director), List.of(Actor.of(actor)),
                List.of(Country.of(country)), null, List.of(new Rating("TMDB", tmdbScore, 1000)));
    }

    private static MovieQuery query(String title) {
        return new MovieQuery(title, null, null);
    }

    private LetterboxdLibrary library(Integer... watchedTmdbIds) {
        return new LetterboxdLibrary(profile, Set.of(watchedTmdbIds));
    }

    // --- the six required cases ------------------------------------------------

    @Test
    void identifiedAndUnseenCandidateEntersTheRanking() {
        MovieQuery q = query("Perfect Days");
        Movie m = movie(100, "Perfect Days", "Wim Wenders", "Drama", "Koji Yakusho",
                "Japan", "2023-12-25", 8.0);

        RecommendationResult result = serviceWith(library(),
                queries -> List.of(MovieIdentificationResult.of(q, MovieMatch.identified(m))))
                .recommend(List.of(q));

        assertThat(result.ranked()).extracting(c -> c.movie().tmdbId()).containsExactly(100);
        assertThat(result.ranked().get(0).score().qualityScore().hasValue()).isTrue();
        assertThat(result.ranked().get(0).reason().text()).isNotBlank();
        assertThat(result.ranked().get(0).similaritySignal()).isNotNull(); // computed, never null
        assertThat(result.excluded()).isEmpty();
    }

    @Test
    void carriesASimilaritySignalWithMatchesFromTheHistory() {
        MovieQuery q = query("Sequel");
        Movie m = new Movie(200, "tt200", "Sequel", "Sequel", "2026-01-01", 120, "o",
                List.of(Genre.of("Crimen"), Genre.of("Suspense")), Director.of("Fav Director"),
                List.of(Actor.of("A")), List.of(Country.of("France")), null,
                List.of(new Rating("TMDB", 7.0, 1000)),
                List.of(new com.magomez.androidapps.movierec.model.SimilarMovie(9, "Zodiac", 2007)));
        LetterboxdLibrary lib = new LetterboxdLibrary(profile, List.of(
                new com.magomez.androidapps.movierec.scoring.letterboxd.WatchedMovie(
                        9, "Zodiac", 9.0, List.of("Crimen", "Suspense"), "David Fincher")));

        RecommendationResult result = serviceWith(lib,
                queries -> List.of(MovieIdentificationResult.of(q, MovieMatch.identified(m))))
                .recommend(List.of(q));

        var signal = result.ranked().get(0).similaritySignal();
        assertThat(signal.matchCount()).isEqualTo(1);
        assertThat(signal.matches().get(0).watchedTitle()).isEqualTo("Zodiac");
        assertThat(signal.similarityStrength()).isEqualTo(0.4); // 2 shared genres, single match
    }

    @Test
    void carriesTheProfileTastePatternsThrough() {
        MovieQuery q = query("Anything");
        Movie m = movie(1, "Anything", "Dir", "Drama", "Actor", "France", "2023-01-01", 7.0);
        UserTasteProfile withPattern = profile.withPatterns(List.of(new com.magomez.androidapps.movierec.scoring.TastePattern(
                "KOREAN_THRILLER", "Thriller coreano", "desc", 0.5,
                List.of("Suspense"), List.of(), List.of(), List.of("evidencia"))));

        RecommendationResult result = serviceWith(new LetterboxdLibrary(withPattern, Set.<Integer>of()),
                queries -> List.of(MovieIdentificationResult.of(q, MovieMatch.identified(m))))
                .recommend(List.of(q));

        assertThat(result.profilePatterns()).extracting(p -> p.id()).containsExactly("KOREAN_THRILLER");
    }

    @Test
    void identifiedButAlreadyWatchedCandidateIsExcluded() {
        MovieQuery q = query("Oldboy");
        Movie m = movie(670, "Oldboy", "Park Chan-wook", "Drama", "Choi Min-sik",
                "South Korea", "2003-11-21", 8.4);

        RecommendationResult result = serviceWith(library(670),
                queries -> List.of(MovieIdentificationResult.of(q, MovieMatch.identified(m))))
                .recommend(List.of(q));

        assertThat(result.ranked()).isEmpty();
        assertThat(result.excluded()).singleElement().satisfies(e -> {
            assertThat(e.reason()).isEqualTo(ExclusionReason.ALREADY_WATCHED);
            assertThat(e.tmdbId()).isEqualTo(670);
            assertThat(e.title()).isEqualTo("Oldboy");
        });
    }

    @Test
    void ambiguousCandidateIsExcluded() {
        MovieQuery q = query("Crash");

        RecommendationResult result = serviceWith(library(),
                queries -> List.of(MovieIdentificationResult.of(q,
                        MovieMatch.ambiguous(List.of("Crash (1996)", "Crash (2004)")))))
                .recommend(List.of(q));

        assertThat(result.ranked()).isEmpty();
        assertThat(result.excluded()).singleElement().satisfies(e -> {
            assertThat(e.reason()).isEqualTo(ExclusionReason.AMBIGUOUS);
            assertThat(e.tmdbId()).isNull();
            assertThat(e.candidates()).containsExactly("Crash (1996)", "Crash (2004)");
        });
    }

    @Test
    void notFoundCandidateIsExcluded() {
        MovieQuery q = query("A TV Show");

        RecommendationResult result = serviceWith(library(),
                queries -> List.of(MovieIdentificationResult.of(q, MovieMatch.notFound())))
                .recommend(List.of(q));

        assertThat(result.ranked()).isEmpty();
        assertThat(result.excluded()).singleElement().satisfies(e ->
                assertThat(e.reason()).isEqualTo(ExclusionReason.NOT_FOUND));
    }

    @Test
    void twoCandidatesResolvingToTheSameTmdbIdKeepOnlyOne() {
        MovieQuery first = query("Parasite");
        MovieQuery second = query("Gisaengchung");
        Movie sameMovie = movie(496243, "Parasite", "Bong Joon-ho", "Drama", "Song Kang-ho",
                "South Korea", "2019-05-30", 8.5);

        RecommendationResult result = serviceWith(library(),
                queries -> List.of(
                        MovieIdentificationResult.of(first, MovieMatch.identified(sameMovie)),
                        MovieIdentificationResult.of(second, MovieMatch.identified(sameMovie))))
                .recommend(List.of(first, second));

        assertThat(result.ranked()).hasSize(1);
        assertThat(result.ranked().get(0).title()).isEqualTo("Parasite");
        assertThat(result.excluded()).singleElement().satisfies(e -> {
            assertThat(e.reason()).isEqualTo(ExclusionReason.DUPLICATE_CANDIDATE);
            assertThat(e.tmdbId()).isEqualTo(496243);
            assertThat(e.title()).isEqualTo("Gisaengchung");
        });
    }

    @Test
    void rankingIsOrderedByPersonalMatchScoreDescending() {
        MovieQuery high = query("High Match");
        MovieQuery mid = query("Mid Match");
        MovieQuery low = query("Low Match");
        // High: every dimension is a preference -> personal affinity 100.
        Movie highMovie = movie(1, "High Match", "Fav Director", "Drama", "Fav Actor",
                "France", "2020-01-01", 7.0);
        // Mid: only the genre is a preference.
        Movie midMovie = movie(2, "Mid Match", "Other Director", "Drama", "Other Actor",
                "Spain", "1999-01-01", 8.0);
        // Low: nothing matches the profile.
        Movie lowMovie = movie(3, "Low Match", "Nobody", "Western", "Nobody", "Nowhere",
                "1975-01-01", 9.0);

        RecommendationResult result = serviceWith(library(),
                queries -> List.of(
                        MovieIdentificationResult.of(high, MovieMatch.identified(highMovie)),
                        MovieIdentificationResult.of(mid, MovieMatch.identified(midMovie)),
                        MovieIdentificationResult.of(low, MovieMatch.identified(lowMovie))))
                .recommend(List.of(mid, low, high));

        assertThat(result.ranked()).extracting(ScoredCandidate::title)
                .containsExactly("High Match", "Mid Match", "Low Match");
        List<Double> scores = result.ranked().stream()
                .map(c -> c.score().value().orElseThrow())
                .toList();
        assertThat(scores).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void candidateWithTrustedQualityIsRankedAndOneWithoutGoesToAffinityOnly() {
        MovieQuery rated = query("Rated");
        MovieQuery unrated = query("Unrated");
        Movie ratedMovie = movie(1, "Rated", "Fav Director", "Drama", "Fav Actor",
                "France", "2020-01-01", 6.0); // Rating voteCount 1000 -> trusted QUALITY
        Movie unratedMovie = new Movie(2, "tt2", "Unrated", "Unrated", "2020-01-01", 120, "o",
                List.of(Genre.of("Drama")), Director.of("Fav Director"),
                List.of(Actor.of("Fav Actor")), List.of(Country.of("France")), null, List.of());

        RecommendationResult result = serviceWith(library(),
                queries -> List.of(
                        MovieIdentificationResult.of(rated, MovieMatch.identified(ratedMovie)),
                        MovieIdentificationResult.of(unrated, MovieMatch.identified(unratedMovie))))
                .recommend(List.of(unrated, rated));

        assertThat(result.ranked()).extracting(ScoredCandidate::title).containsExactly("Rated");
        assertThat(result.ranked().get(0).score().value()).isPresent();

        assertThat(result.affinityOnly()).extracting(ScoredCandidate::title).containsExactly("Unrated");
        assertThat(result.affinityOnly().get(0).score().value()).isEmpty();
        assertThat(result.affinityOnly().get(0).score().personalAffinity()).isPresent();
    }

    @Test
    void aStrongAffinityOnlyCandidateOutranksAWeakQualityBackedOneInTheOneUnifiedList() {
        MovieQuery unratedQuery = query("Unrated High Fit");
        MovieQuery ratedQuery = query("Rated Low Fit");
        // full director/genre/actor match, but no trustworthy quality at all
        Movie unratedHighFit = new Movie(10, "tt10", "Unrated High Fit", "Unrated High Fit",
                "2026-01-01", 120, "o", List.of(Genre.of("Drama")), Director.of("Fav Director"),
                List.of(Actor.of("Fav Actor")), List.of(Country.of("France")), null, List.of());
        // no taste match at all, but a (poor) trustworthy quality rating
        Movie ratedLowFit = new Movie(20, "tt20", "Rated Low Fit", "Rated Low Fit",
                "2020-01-01", 120, "o", List.of(Genre.of("Comedy")), Director.of("Nobody"),
                List.of(Actor.of("Nobody")), List.of(Country.of("Nowhere")), null,
                List.of(new Rating("TMDB", 1.0, 1000)));

        RecommendationResult result = serviceWith(library(),
                queries -> List.of(
                        MovieIdentificationResult.of(unratedQuery, MovieMatch.identified(unratedHighFit)),
                        MovieIdentificationResult.of(ratedQuery, MovieMatch.identified(ratedLowFit))))
                .recommend(List.of(unratedQuery, ratedQuery));

        // one unified list — never hidden below the (weak) quality-backed candidate
        assertThat(result.recommendations()).extracting(ScoredCandidate::title)
                .containsExactly("Unrated High Fit", "Rated Low Fit");

        ScoredCandidate strongEstimate = result.recommendations().get(0);
        assertThat(strongEstimate.score().value()).isEmpty(); // never a confirmed PMS...
        assertThat(strongEstimate.score().estimatedValue()).hasValue(65.0); // ...but a real estimate: 100*0.65

        ScoredCandidate weakConfirmed = result.recommendations().get(1);
        assertThat(weakConfirmed.score().value()).isPresent();
        assertThat(weakConfirmed.score().estimatedValue()).isEqualTo(weakConfirmed.score().value());

        // the filtered views still separate them by whether the score is confirmed
        assertThat(result.ranked()).extracting(ScoredCandidate::title).containsExactly("Rated Low Fit");
        assertThat(result.affinityOnly()).extracting(ScoredCandidate::title).containsExactly("Unrated High Fit");
    }

    @Test
    void aCandidateWhoseOnlyRatingHasTooFewVotesIsAffinityOnly() {
        MovieQuery q = query("Thin");
        Movie thin = new Movie(3, "tt3", "Thin", "Thin", "2026-01-01", 120, "o",
                List.of(Genre.of("Drama")), Director.of("Fav Director"), List.of(Actor.of("Fav Actor")),
                List.of(Country.of("France")), null, List.of(new Rating("TMDB", 9.0, 9))); // 9 votes

        RecommendationResult result = serviceWith(library(),
                queries -> List.of(MovieIdentificationResult.of(q, MovieMatch.identified(thin))))
                .recommend(List.of(q));

        assertThat(result.ranked()).isEmpty();
        assertThat(result.affinityOnly()).extracting(ScoredCandidate::title).containsExactly("Thin");
    }
}
