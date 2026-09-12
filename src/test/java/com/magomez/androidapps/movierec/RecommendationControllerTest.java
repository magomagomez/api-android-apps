package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.controller.RecommendationController;
import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.recommendation.ExcludedCandidate;
import com.magomez.androidapps.movierec.recommendation.RecommendationReason;
import com.magomez.androidapps.movierec.recommendation.RecommendationResult;
import com.magomez.androidapps.movierec.recommendation.RecommendationService;
import com.magomez.androidapps.movierec.recommendation.ScoredCandidate;
import com.magomez.androidapps.movierec.scoring.PersonalAffinityCalculator;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScoreCalculator;
import com.magomez.androidapps.movierec.scoring.QualityScoreCalculator;
import com.magomez.androidapps.movierec.scoring.RatingNormalizer;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint contract test for {@code POST /api/recommendations}. Uses a hand-written
 * {@link RecommendationService} stub (concrete class; the CI JVM cannot always instrument
 * it) but the real {@code RecommendationApiMapper}, and a standalone MockMvc so no Spring
 * context / database is needed. No external API is called.
 */
class RecommendationControllerTest {

    private final PersonalMatchScoreCalculator scoreCalculator = new PersonalMatchScoreCalculator(
            new QualityScoreCalculator(new RatingNormalizer()),
            new PersonalAffinityCalculator());
    private final UserTasteProfile profile = new UserTasteProfile(
            Set.of("Coralie Fargeat"), Set.of("Terror"), Set.of("Demi Moore"),
            Set.of("France"), Set.of(2020));

    private List<MovieQuery> lastCandidates;
    private Function<List<MovieQuery>, RecommendationResult> handler = q -> new RecommendationResult(0, List.of(), List.of());
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RecommendationService stub = new RecommendationService(null, null, null, null, null, null, null) {
            @Override
            public RecommendationResult recommend(List<MovieQuery> candidates) {
                lastCandidates = candidates;
                return handler.apply(candidates);
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new RecommendationController(stub)).build();
    }

    private PersonalMatchScore scoreOf(Movie movie) {
        return scoreCalculator.calculate(movie, profile);
    }

    @Test
    void returnsTheRankingAndTheExclusionsAsJson() throws Exception {
        Movie substance = new Movie(1064213, "tt17526714", "The Substance", "The Substance",
                "2024-09-07", 141, "Una estrella en decadencia usa una sustancia que crea una versión mejor de sí misma.",
                List.of(new Genre(27, "Terror")), new Director(1, "Coralie Fargeat"),
                List.of(new Actor(1, "Demi Moore")), List.of(new Country("FR", "France")),
                "https://image.tmdb.org/t/p/w500/thesubstance.jpg",
                List.of(new Rating("TMDB", 7.3, 6000), new Rating("IMDb", 7.3, 150000)));
        RecommendationReason reason = new RecommendationReason(
                "Te la recomendaría porque su director, Coralie Fargeat, es uno de tus directores de cabecera.",
                List.of("Has valorado muy positivamente a Coralie Fargeat, uno de tus directores de cabecera"),
                List.of(com.magomez.androidapps.movierec.recommendation.PatternSignal.of(
                        "PSYCHOLOGICAL_HORROR", "Terror psicológico", 0.64, 0.8)));

        var similaritySignal = new com.magomez.androidapps.movierec.recommendation.SimilaritySignal(
                0.72, 1, List.of(new com.magomez.androidapps.movierec.recommendation.SimilarityMatch(
                        "American Psycho", 1359, 9.0, false, List.of("Crimen", "Drama"), 0.4,
                        com.magomez.androidapps.movierec.recommendation.SimilarityMatch.Source.SHARED_GENRES)));

        var accoladeSignal = new com.magomez.androidapps.movierec.recommendation.AccoladeSignal(
                0.9, List.of("Cuenta con varios premios y nominaciones a sus espaldas."),
                new com.magomez.androidapps.movierec.model.AwardsTally(0, 0, 2, 6),
                List.of(com.magomez.androidapps.movierec.model.FestivalAchievement.selection(
                        "Sundance", 2024, "Midnight")));

        handler = candidates -> new RecommendationResult(3,
                List.of(new ScoredCandidate("The Substance", substance, scoreOf(substance),
                        similaritySignal, accoladeSignal, reason, null)),
                List.of(
                        ExcludedCandidate.alreadyWatched("Oldboy", 670),
                        ExcludedCandidate.ambiguous("Crash", List.of("Crash (1996)", "Crash (2004)"))),
                List.of(new com.magomez.androidapps.movierec.scoring.TastePattern(
                        "PSYCHOLOGICAL_HORROR", "Terror psicológico", "Terror + suspense/misterio/drama.", 0.64,
                        List.of("Terror", "Suspense"), List.of("Ari Aster"), List.of("Hereditary"),
                        List.of("8 de tus 120 películas mejor valoradas encajan"))));

        String body = """
                {
                  "movies": [
                    {"title": "The Substance", "year": 2024, "director": "Coralie Fargeat"},
                    {"title": "Oldboy", "year": 2003, "director": "Park Chan-wook"},
                    {"title": "Crash", "director": null}
                  ]
                }
                """;

        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCandidates").value(3))
                .andExpect(jsonPath("$.summary.identified").value(2)) // The Substance (ranked) + Oldboy (watched)
                .andExpect(jsonPath("$.summary.notFound").value(0))
                .andExpect(jsonPath("$.summary.ambiguous").value(1))
                .andExpect(jsonPath("$.summary.alreadyWatched").value(1))
                .andExpect(jsonPath("$.summary.duplicateCandidate").value(0))
                .andExpect(jsonPath("$.summary.recommended").value(1))
                .andExpect(jsonPath("$.summary.withQualityScore").value(1))
                .andExpect(jsonPath("$.summary.withCompletePersonalMatchScore").value(1))
                .andExpect(jsonPath("$.recommendations[0].position").value(1))
                .andExpect(jsonPath("$.recommendations[0].title").value("The Substance"))
                .andExpect(jsonPath("$.recommendations[0].tmdbId").value(1064213))
                .andExpect(jsonPath("$.recommendations[0].imdbId").value("tt17526714"))
                .andExpect(jsonPath("$.recommendations[0].posterUrl").value(
                        "https://image.tmdb.org/t/p/w500/thesubstance.jpg"))
                .andExpect(jsonPath("$.recommendations[0].director").value("Coralie Fargeat"))
                .andExpect(jsonPath("$.recommendations[0].year").value(2024))
                .andExpect(jsonPath("$.recommendations[0].synopsis").value(
                        "Una estrella en decadencia usa una sustancia que crea una versión mejor de sí misma."))
                .andExpect(jsonPath("$.recommendations[0].actors[0]").value("Demi Moore"))
                .andExpect(jsonPath("$.recommendations[0].genres[0]").value("Terror"))
                .andExpect(jsonPath("$.recommendations[0].ratings[0].source").value("TMDB"))
                .andExpect(jsonPath("$.recommendations[0].ratings[0].score").value(7.3))
                .andExpect(jsonPath("$.recommendations[0].ratings[0].scale").value(10))
                .andExpect(jsonPath("$.recommendations[0].ratings[0].voteCount").value(6000))
                .andExpect(jsonPath("$.recommendations[0].ratings[1].source").value("IMDb"))
                .andExpect(jsonPath("$.recommendations[0].ratings[1].voteCount").value(150000))
                .andExpect(jsonPath("$.recommendations[0].personalMatchScore").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.recommendations[0].qualityConfirmed").value(true))
                .andExpect(jsonPath("$.recommendations[0].festivalRecognition[0].festival").value("Sundance"))
                .andExpect(jsonPath("$.recommendations[0].festivalRecognition[0].type").value("SELECTION"))
                .andExpect(jsonPath("$.recommendations[0].accoladeHighlights[0]").value(
                        "Cuenta con varios premios y nominaciones a sus espaldas."))
                .andExpect(jsonPath("$.recommendations[0].recommendationReason").value(
                        "Te la recomendaría porque su director, Coralie Fargeat, es uno de tus directores de cabecera."))
                .andExpect(jsonPath("$.recommendations[0].reasons[0]").value(
                        "Has valorado muy positivamente a Coralie Fargeat, uno de tus directores de cabecera"))
                .andExpect(jsonPath("$.recommendations[0].note").value(nullValue()))
                .andExpect(jsonPath("$.excluded[0].title").value("Oldboy"))
                .andExpect(jsonPath("$.excluded[0].tmdbId").value(670))
                .andExpect(jsonPath("$.excluded[0].reason").value("ALREADY_WATCHED"))
                .andExpect(jsonPath("$.excluded[1].title").value("Crash"))
                .andExpect(jsonPath("$.excluded[1].reason").value("AMBIGUOUS"))
                .andExpect(jsonPath("$.excluded[1].candidates[0]").value("Crash (1996)"))
                .andExpect(jsonPath("$.profilePatterns").doesNotExist());

        assertEquals(3, lastCandidates.size());
        assertEquals(2024, lastCandidates.get(0).year());
        assertEquals("Coralie Fargeat", lastCandidates.get(0).director());
        assertEquals("Crash", lastCandidates.get(2).title());
    }

    @Test
    void rejectsNonJsonContentType() throws Exception {
        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("The Substance;2024"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void missingTitleIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movies\":[{\"year\":2020}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anEmptyCandidateListYieldsAnEmptyRanking() throws Exception {
        handler = candidates -> new RecommendationResult(0, List.of(), List.of());

        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movies\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCandidates").value(0))
                .andExpect(jsonPath("$.recommendations.length()").value(0))
                .andExpect(jsonPath("$.excluded.length()").value(0));
    }

    @Test
    void aCandidateWithoutTrustworthyQualityStillAppearsWithAnEstimatedScoreInstead() throws Exception {
        Movie unreleased = new Movie(999, "tt999", "Future Film", "Future Film",
                "2026-05-01", 110, "Una premisa perturbadora en un pueblo aislado.",
                List.of(new Genre(18, "Drama"), new Genre(27, "Terror")),
                new Director(5, "Some Director"), List.of(new Actor(7, "Some Actor")),
                List.of(new Country("KR", "South Korea")), null,
                List.of()); // no ratings -> no trusted QUALITY
        PersonalMatchScore score = scoreOf(unreleased);
        RecommendationReason reason = new RecommendationReason(
                "Te la recomendaría porque encaja con tu gusto por Drama.", List.of("Géneros que frecuentas: Drama"));

        handler = candidates -> new RecommendationResult(1,
                List.of(new ScoredCandidate("Future Film", unreleased, score, reason, null)),
                List.of(), List.of());

        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movies\":[{\"title\":\"Future Film\",\"year\":2026}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.recommended").value(1))
                .andExpect(jsonPath("$.summary.withCompletePersonalMatchScore").value(0))
                .andExpect(jsonPath("$.recommendations[0].position").value(1))
                .andExpect(jsonPath("$.recommendations[0].title").value("Future Film"))
                .andExpect(jsonPath("$.recommendations[0].personalMatchScore").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.recommendations[0].qualityConfirmed").value(false))
                .andExpect(jsonPath("$.recommendations[0].ratings.length()").value(0));
    }
}
