package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.model.SimilarMovie;
import com.magomez.androidapps.movierec.recommendation.RecommendationReason;
import com.magomez.androidapps.movierec.recommendation.RecommendationReasoner;
import com.magomez.androidapps.movierec.recommendation.SimilaritySignal;
import com.magomez.androidapps.movierec.recommendation.SimilaritySignalCalculator;
import com.magomez.androidapps.movierec.scoring.PersonalAffinityCalculator;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScoreCalculator;
import com.magomez.androidapps.movierec.scoring.QualityScoreCalculator;
import com.magomez.androidapps.movierec.scoring.RatingNormalizer;
import com.magomez.androidapps.movierec.scoring.TastePattern;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibrary;
import com.magomez.androidapps.movierec.scoring.letterboxd.WatchedMovie;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The recommendation reason is deterministic and grounded: it only states what the
 * profile / TMDB data support, names similar movies from the user's own history, and
 * follows the priority similar &gt; director &gt; pattern &gt; genre &gt; actor &gt; quality.
 */
class RecommendationReasonerTest {

    private final RecommendationReasoner reasoner = new RecommendationReasoner();
    private final PersonalMatchScoreCalculator scoreCalculator = new PersonalMatchScoreCalculator(
            new QualityScoreCalculator(new RatingNormalizer()),
            new PersonalAffinityCalculator());

    private static Movie movie(String title, Director director, List<Genre> genres, List<Actor> actors,
                               List<Country> countries, String overview, double tmdbScore) {
        return new Movie(1, "tt1", title, title, "2019-05-30", 120, overview,
                genres, director, actors, countries, null, List.of(new Rating("TMDB", tmdbScore, 1000)),
                List.of());
    }

    private RecommendationReason explain(Movie m, UserTasteProfile profile, LetterboxdLibrary library) {
        PersonalMatchScore score = scoreCalculator.calculate(m, profile);
        SimilaritySignal similaritySignal = new SimilaritySignalCalculator().calculate(m, library);
        return reasoner.explain(m, profile, score, similaritySignal, library);
    }

    private static LetterboxdLibrary library(UserTasteProfile profile, WatchedMovie... watched) {
        return new LetterboxdLibrary(profile, List.of(watched));
    }

    private static TastePattern pattern(String id, String name, double strength) {
        return new TastePattern(id, name, "desc", strength,
                List.of("Suspense"), List.of("Bong Joon-ho"), List.of("Oldboy"),
                List.of("14 de tus 300 películas mejor valoradas (4,7%) encajan"));
    }

    @Test
    void namesADirectorThatIsAProfileFavourite() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of("Bong Joon-ho", 1.0), Map.of("Suspense", 0.9), Map.of(), Map.of(), Map.of());
        Movie m = movie("Parasite", Director.of("Bong Joon-ho"),
                List.of(Genre.of("Suspense")), List.of(), List.of(Country.of("Nowhere")),
                "Una familia pobre se infiltra en la casa de una familia rica.", 8.5);

        RecommendationReason reason = explain(m, profile, library(profile));

        assertThat(reason.text()).contains("Bong Joon-ho").contains("cabecera");
        assertThat(reason.reasons()).anyMatch(r -> r.contains("Bong Joon-ho"));
    }

    @Test
    void doesNotClaimADirectorFavouriteWhenTheProfileDoesNotBackIt() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of("Someone Else", 1.0), Map.of("Comedia", 1.0), Map.of(), Map.of(), Map.of());
        Movie m = movie("Random", Director.of("Unknown Director"),
                List.of(Genre.of("Western")), List.of(), List.of(Country.of("Nowhere")),
                "A lone gunman rides into town.", 6.0);

        RecommendationReason reason = explain(m, profile, library(profile));

        assertThat(reason.text()).doesNotContain("Unknown Director");
        assertThat(reason.reasons()).noneMatch(r -> r.contains("Unknown Director"));
    }

    @Test
    void namesSimilarMoviesThatAreInTheUserHistoryWhenTheMatchIsStrong() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of("Quentin Tarantino", 0.9), Map.of("Crimen", 0.8), Map.of(), Map.of(), Map.of());
        Movie m = movie("Kill Bill", Director.of("Quentin Tarantino"),
                List.of(Genre.of("Crimen"), Genre.of("Acción")), List.of(), List.of(Country.of("Nowhere")),
                "Una asesina busca venganza.", 8.0)
                .withSimilarMovies(List.of(
                        new SimilarMovie(500, "Reservoir Dogs", 1992),
                        new SimilarMovie(999, "Unseen Movie", 2000)));

        // Reservoir Dogs: same director -> strong match even without shared genres
        LetterboxdLibrary library = library(profile,
                new WatchedMovie(500, "Reservoir Dogs", 10.0, List.of(), "Quentin Tarantino"));

        RecommendationReason reason = explain(m, profile, library);

        assertThat(reason.text()).contains("Reservoir Dogs").doesNotContain("Unseen Movie");
        assertThat(reason.reasons()).anyMatch(r -> r.contains("Reservoir Dogs"));
    }

    @Test
    void acceptsASimilarMatchWithTwoSharedNicheGenresAndRejectsAWeakerOne() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of(), Map.of("Crimen", 0.9), Map.of(), Map.of(), Map.of());
        Movie m = movie("Candidate", Director.of("Boots Riley"),
                List.of(Genre.of("Comedia"), Genre.of("Crimen"), Genre.of("Suspense")), List.of(),
                List.of(Country.of("USA")), "overview", 7.0)
                .withSimilarMovies(List.of(
                        new SimilarMovie(1, "Shrek 2", 2004),
                        new SimilarMovie(2, "Heist", 1998)));
        LetterboxdLibrary library = library(profile,
                // only broad genres shared (Comedia) -> rejected
                new WatchedMovie(1, "Shrek 2", 7.0, List.of("Comedia", "Animación", "Familia"), "Andrew Adamson"),
                // 2 niche genres shared (Crimen, Suspense) -> accepted
                new WatchedMovie(2, "Heist", 8.0, List.of("Crimen", "Suspense", "Comedia"), "Joel Coen"));

        RecommendationReason reason = explain(m, profile, library);

        assertThat(reason.text()).contains("Heist").doesNotContain("Shrek 2");
    }

    @Test
    void rejectsASimilarMatchThatSharesOnlyBroadGenres() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of(), Map.of("Fantasía", 0.9), Map.of(), Map.of(), Map.of());
        Movie m = movie("Wicker", Director.of("Eleanor Wilson"),
                List.of(Genre.of("Comedia"), Genre.of("Fantasía"), Genre.of("Aventura")), List.of(),
                List.of(Country.of("USA")), "overview", 7.0)
                .withSimilarMovies(List.of(new SimilarMovie(1, "El señor de los anillos", 2001)));
        LetterboxdLibrary library = library(profile,
                new WatchedMovie(1, "El señor de los anillos", 10.0,
                        List.of("Aventura", "Fantasía", "Acción"), "Peter Jackson"));

        RecommendationReason reason = explain(m, profile, library);

        assertThat(reason.text()).doesNotContain("señor de los anillos");
    }

    @Test
    void explainsUsingANarrativePatternFromTheProfile() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of(), Map.of("Suspense", 0.9), Map.of(), Map.of(), Map.of())
                .withPatterns(List.of(pattern("KOREAN_THRILLER", "Thriller coreano", 0.72)));
        Movie m = movie("Decision to Leave", Director.of("Park Chan-wook"),
                List.of(Genre.of("Suspense"), Genre.of("Misterio")), List.of(),
                List.of(Country.of("Corea del Sur")),
                "Un detective se obsesiona con la viuda de un caso.", 7.8);

        RecommendationReason reason = explain(m, profile, library(profile));

        assertThat(reason.text()).contains("thriller coreano")
                .contains("un patrón muy presente entre tus películas mejor valoradas");
        assertThat(reason.reasons()).anyMatch(r -> r.contains("«Thriller coreano»")
                && r.contains("un patrón muy presente entre tus películas mejor valoradas"));
        // no raw numbers leak into the structured reasons
        assertThat(reason.reasons()).noneMatch(r -> r.contains("0.72") || r.contains("fuerza de perfil"));
    }

    // --- patternMatch / relevance --------------------------------------------

    @Test
    void patternSignalsCarryProfileStrengthPatternMatchAndRelevance() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of(), Map.of("Suspense", 0.9), Map.of(), Map.of(), Map.of())
                .withPatterns(List.of(pattern("KOREAN_THRILLER", "Thriller coreano", 0.5)));
        Movie m = movie("A", Director.of("Dir"),
                List.of(Genre.of("Suspense"), Genre.of("Misterio")), List.of(),
                List.of(Country.of("Corea del Sur")), "overview", 7.5);

        var signals = explain(m, profile, library(profile)).patternSignals();

        assertThat(signals).singleElement().satisfies(s -> {
            assertThat(s.id()).isEqualTo("KOREAN_THRILLER");
            assertThat(s.profileStrength()).isEqualTo(0.5);
            assertThat(s.patternMatch()).isEqualTo(1.0); // 2 trigger genres
            assertThat(s.relevance()).isEqualTo(0.5); // 0.5 * 1.0
        });
    }

    @Test
    void patternMatchIsZeroSoNoSignalWhenTheFilmDoesNotFit() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of(), Map.of("Comedia", 0.9), Map.of(), Map.of(), Map.of())
                .withPatterns(List.of(pattern("KOREAN_THRILLER", "Thriller coreano", 0.5)));
        Movie m = movie("A", Director.of("Dir"), List.of(Genre.of("Comedia")), List.of(),
                List.of(Country.of("USA")), "overview", 7.0);

        assertThat(explain(m, profile, library(profile)).patternSignals()).isEmpty();
    }

    @Test
    void reasonerLeadsWithThePatternOfHighestRelevanceNotHighestStrength() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of())
                .withPatterns(List.of(
                        pattern("KOREAN_THRILLER", "Thriller coreano", 0.50),      // lower strength
                        pattern("THOUGHT_PROVOKING", "Cine que deja poso", 0.55))); // higher strength
        // fits KOREAN fully (patternMatch 1.0 -> relevance 0.50) but THOUGHT only 0.8
        // (Drama + Historia -> patternMatch 0.8 -> relevance 0.44)
        Movie m = movie("A", Director.of("Dir"),
                List.of(Genre.of("Drama"), Genre.of("Historia"), Genre.of("Suspense"), Genre.of("Crimen")),
                List.of(), List.of(Country.of("Corea del Sur")), "overview", 7.5);

        RecommendationReason reason = explain(m, profile, library(profile));

        assertThat(reason.patternSignals()).extracting(s -> s.id())
                .containsExactly("KOREAN_THRILLER", "THOUGHT_PROVOKING"); // ordered by relevance
        assertThat(reason.text()).contains("thriller coreano").doesNotContain("dejan poso");
    }

    @Test
    void patternSignalsAreDeterministic() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of())
                .withPatterns(List.of(
                        pattern("KOREAN_THRILLER", "Thriller coreano", 0.5),
                        pattern("UNCOMFORTABLE_CINEMA", "Cine incómodo", 0.5)));
        Movie m = movie("A", Director.of("Dir"),
                List.of(Genre.of("Drama"), Genre.of("Suspense"), Genre.of("Crimen")), List.of(),
                List.of(Country.of("Corea del Sur")), "overview", 7.0);

        assertThat(explain(m, profile, library(profile)).patternSignals())
                .isEqualTo(explain(m, profile, library(profile)).patternSignals());
    }

    @Test
    void doesNotMentionAPatternTheProfileDoesNotHave() {
        // candidate IS a black comedy, but the user's profile has no such pattern
        UserTasteProfile profile = new UserTasteProfile(
                Map.of(), Map.of("Comedia", 0.9), Map.of(), Map.of(), Map.of());
        Movie m = movie("The Menu", Director.of("Mark Mylod"),
                List.of(Genre.of("Comedia"), Genre.of("Terror")), List.of(),
                List.of(Country.of("Nowhere")), "Una cena de lujo se tuerce.", 7.2);

        RecommendationReason reason = explain(m, profile, library(profile));

        assertThat(reason.text()).doesNotContain("comedia negra");
        assertThat(reason.reasons()).noneMatch(r -> r.contains("Patrón de gusto"));
    }

    @Test
    void priorityIsSimilarThenDirectorThenPatternThenGenreThenActor() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of("Bong Joon-ho", 1.0), Map.of("Suspense", 1.0), Map.of("Song Kang-ho", 1.0),
                Map.of(), Map.of())
                .withPatterns(List.of(pattern("KOREAN_THRILLER", "Thriller coreano", 0.8)));
        Movie m = movie("Memories of Murder", Director.of("Bong Joon-ho"),
                List.of(Genre.of("Suspense"), Genre.of("Crimen")),
                List.of(Actor.of("Song Kang-ho")), List.of(Country.of("Corea del Sur")),
                "Dos detectives rurales investigan una serie de asesinatos.", 8.1)
                .withSimilarMovies(List.of(new SimilarMovie(700, "Zodiac", 2007)));
        LetterboxdLibrary library = library(profile,
                new WatchedMovie(700, "Zodiac", 9.0, List.of("Suspense", "Crimen"), "David Fincher"));

        String text = explain(m, profile, library).text();

        int similar = text.indexOf("Zodiac");
        int director = text.indexOf("Bong Joon-ho");
        int patternPos = text.indexOf("thriller coreano");
        int genre = text.indexOf("tu gusto por");
        assertThat(similar).isGreaterThanOrEqualTo(0);
        assertThat(director).isGreaterThan(similar);
        assertThat(patternPos).isGreaterThan(director);
        // only the top 3 signals make the text, so genre/actor are dropped here
        assertThat(genre).isEqualTo(-1);
        assertThat(text).doesNotContain("Song Kang-ho");
    }

    @Test
    void actorIsOnlyASecondaryTestimonialMentionNeverALead() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of("Hirokazu Koreeda", 1.0), Map.of(), Map.of("Song Kang-ho", 1.0),
                Map.of(), Map.of());
        Movie m = movie("Broker", Director.of("Hirokazu Koreeda"),
                List.of(Genre.of("Drama")), List.of(Actor.of("Song Kang-ho")),
                List.of(Country.of("Corea del Sur")), "Historia sobre una baby box.", 7.0);

        String text = explain(m, profile, library(profile)).text();

        assertThat(text).startsWith("Te la recomendaría porque su director");
        assertThat(text.indexOf("Song Kang-ho")).isGreaterThan(text.indexOf("Hirokazu Koreeda"));
    }

    @Test
    void whenNothingConnectsItSaysSoInsteadOfInventing() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of("Someone", 1.0), Map.of("Comedia", 1.0), Map.of("Nobody", 1.0),
                Map.of(), Map.of());
        Movie m = movie("Unrelated", Director.of("Nobody At All"),
                List.of(Genre.of("Documental")), List.of(Actor.of("Nadie")),
                List.of(Country.of("Nowhere")), "A quiet documentary about nothing.", 8.6);

        RecommendationReason reason = explain(m, profile, library(profile));

        assertThat(reason.text()).contains("valoración externa");
        assertThat(reason.text()).doesNotContain("Nobody At All");
    }

    @Test
    void isDeterministic() {
        UserTasteProfile profile = new UserTasteProfile(
                Map.of("Bong Joon-ho", 1.0), Map.of("Suspense", 0.9), Map.of(), Map.of(), Map.of())
                .withPatterns(List.of(pattern("KOREAN_THRILLER", "Thriller coreano", 0.7)));
        Movie m = movie("Parasite", Director.of("Bong Joon-ho"), List.of(Genre.of("Suspense")),
                List.of(), List.of(Country.of("Corea del Sur")), "overview", 8.5);

        assertThat(explain(m, profile, library(profile)).text())
                .isEqualTo(explain(m, profile, library(profile)).text());
    }
}
