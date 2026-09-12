package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.scoring.PersonalAffinityCalculator;
import com.magomez.androidapps.movierec.scoring.PersonalAffinitySignals;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScore.Status;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScoreCalculator;
import com.magomez.androidapps.movierec.scoring.QualityScore;
import com.magomez.androidapps.movierec.scoring.QualityScoreCalculator;
import com.magomez.androidapps.movierec.scoring.RatingNormalizer;
import com.magomez.androidapps.movierec.scoring.UnknownRatingSourceException;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PERSONAL MATCH SCORE = {@code quality * 0.35 + personalAffinity * 0.65}, where
 * {@code personalAffinity = director * 0.55 + genre * 0.35 + actor * 0.10}. Country and
 * decade do not take part; actor is a small, testimonial signal.
 */
class PersonalMatchScoreCalculatorTest {

    private final PersonalMatchScoreCalculator calculator = new PersonalMatchScoreCalculator(
            new QualityScoreCalculator(new RatingNormalizer()),
            new PersonalAffinityCalculator());

    private static Movie movie(Director director, List<Genre> genres, List<Actor> actors,
                               String releaseDate, List<Rating> ratings) {
        return new Movie(1, "tt1", "The Movie", "The Movie", releaseDate, 120, "overview",
                genres, director, actors, List.of(Country.of("Nowhere")), null, ratings);
    }

    private static UserTasteProfile profile(Set<String> directors, Set<String> genres,
                                            Set<String> actors) {
        return new UserTasteProfile(directors, genres, actors, Set.of(), Set.of());
    }

    // --- taste-pattern bonus -------------------------------------------------

    @Test
    void aMatchingTastePatternAddsABoundedBonusOnTopOfTheBaseScore() {
        // one movie, two profiles: identical base score, the difference is exactly the bonus
        Movie blackComedy = movie(Director.of("Fav Director"),
                List.of(Genre.of("Comedia"), Genre.of("Terror")), List.of(), "2024-01-01",
                List.of(new Rating("TMDB", 8.0, 1000)));

        UserTasteProfile noPatterns = new UserTasteProfile(
                java.util.Map.of("Fav Director", 1.0), java.util.Map.of("Terror", 1.0),
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
        UserTasteProfile withPattern = noPatterns.withPatterns(List.of(
                new com.magomez.androidapps.movierec.scoring.TastePattern(
                        "BLACK_COMEDY", "Comedia negra", "d", 0.5,
                        List.of(), List.of(), List.of(), List.of())));

        PersonalMatchScore without = calculator.calculate(blackComedy, noPatterns);
        PersonalMatchScore with = calculator.calculate(blackComedy, withPattern);

        assertThat(without.patternBonus()).isZero();
        // BLACK_COMEDY strength 0.5, patternMatch 0.8 (2 triggers) -> relevance 0.4 -> bonus 12.0
        assertThat(with.patternBonus()).isEqualTo(12.0);
        assertThat(with.value().getAsDouble())
                .isEqualTo(without.value().getAsDouble() + 12.0);
    }

    @Test
    void thePatternBonusFollowsPreferenceStrengthNotStrength() {
        // strength is high (mostly prevalence: "you rate a lot of this either way") but
        // preferenceStrength (the real lift over baseline) is near zero: merely common in
        // the whole history, not something the user actually gravitates towards
        Movie blackComedy = movie(Director.of("Fav Director"),
                List.of(Genre.of("Comedia"), Genre.of("Terror")), List.of(), "2024-01-01",
                List.of(new Rating("TMDB", 8.0, 1000)));
        UserTasteProfile taste = new UserTasteProfile(
                java.util.Map.of("Fav Director", 1.0), java.util.Map.of("Terror", 1.0),
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of())
                .withPatterns(List.of(new com.magomez.androidapps.movierec.scoring.TastePattern(
                        "BLACK_COMEDY", "Comedia negra", "d", 0.9, 0.0,
                        List.of(), List.of(), List.of(), List.of())));

        assertThat(calculator.calculate(blackComedy, taste).patternBonus()).isZero();
    }

    @Test
    void thePatternBonusIsCappedAt30Points() {
        // strength 1.0, patternMatch 1.0 (3 trigger genres) -> relevance 1.0 -> bonus = 30
        Movie m = movie(Director.of("x"),
                List.of(Genre.of("Comedia"), Genre.of("Terror"), Genre.of("Horror")),
                List.of(), "2024-01-01", List.of(new Rating("TMDB", 10.0, 1000)));
        UserTasteProfile taste = new UserTasteProfile(
                java.util.Map.of("x", 1.0), java.util.Map.of("Terror", 1.0),
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of())
                .withPatterns(List.of(new com.magomez.androidapps.movierec.scoring.TastePattern(
                        "BLACK_COMEDY", "Comedia negra", "d", 1.0,
                        List.of(), List.of(), List.of(), List.of())));

        assertThat(calculator.calculate(m, taste).patternBonus()).isEqualTo(30.0);
    }

    // --- accolade bonus ---------------------------------------------------------

    @Test
    void anAccoladeSignalAddsABoundedBonusOnTopOfTheBaseScore() {
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(), "2024-01-01", List.of(new Rating("TMDB", 8.0, 1000)));
        UserTasteProfile taste = profile(Set.of("Fav Director"), Set.of("Terror"), Set.of());

        PersonalMatchScore without = calculator.calculate(theMovie, taste);
        PersonalMatchScore with = calculator.calculate(theMovie, taste, 0.6);

        assertThat(without.accoladeBonus()).isZero();
        assertThat(with.accoladeBonus()).isEqualTo(12.0); // 20 * 0.6
        assertThat(with.value().getAsDouble()).isEqualTo(without.value().getAsDouble() + 12.0);
    }

    @Test
    void theAccoladeBonusIsCappedAt20PointsAndIsSmallerThanThePatternBonusCap() {
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(), "2024-01-01", List.of(new Rating("TMDB", 8.0, 1000)));
        UserTasteProfile taste = profile(Set.of("Fav Director"), Set.of("Terror"), Set.of());

        assertThat(calculator.calculate(theMovie, taste, 1.0).accoladeBonus()).isEqualTo(20.0);
    }

    @Test
    void rejectsAnAccoladeStrengthOutsideZeroToOne() {
        Movie theMovie = movie(null, List.of(), List.of(), null, List.of());
        UserTasteProfile taste = UserTasteProfile.empty();

        assertThatThrownBy(() -> calculator.calculate(theMovie, taste, 1.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(theMovie, taste, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noAccoladeBonusWithoutATrustedQualityScore() {
        Movie theMovie = movie(Director.of("x"), List.of(Genre.of("Terror")), List.of(), "2026-01-01", List.of());
        UserTasteProfile taste = profile(Set.of("x"), Set.of("Terror"), Set.of());

        PersonalMatchScore score = calculator.calculate(theMovie, taste, 0.9);

        assertThat(score.accoladeBonus()).isEqualTo(18.0); // still computed and reported...
        assertThat(score.value()).isEmpty(); // ...but never applied without a trusted quality
    }

    // --- the formula ----------------------------------------------------------

    @Test
    void quality80AndAffinity90Give865() {
        // signals: director 100, genre 100, actor 0 -> 100*0.55 + 100*0.35 + 0*0.10 = 90
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(), "2024-01-01", List.of(new Rating("TMDB", 8.0, 1000)));
        UserTasteProfile taste = profile(Set.of("Fav Director"), Set.of("Terror"), Set.of());

        PersonalMatchScore score = calculator.calculate(theMovie, taste);

        assertThat(score.status()).isEqualTo(Status.COMPLETE);
        assertThat(score.qualityScore().value()).hasValue(80.0);
        assertThat(score.personalAffinity()).hasValue(90.0);
        assertThat(score.value()).hasValue(86.5); // 80*0.35 + 90*0.65
    }

    @Test
    void aSoleActorMatchBarelyMovesTheScore() {
        // director 0, genre 0, actor 50 (1 of 2) -> 0 + 0 + 5 = 5.0  (testimonial only)
        Movie theMovie = movie(null, List.of(),
                List.of(Actor.of("Fav Actor"), Actor.of("Other")), "2024-01-01",
                List.of(new Rating("TMDB", 8.0, 1000)));
        UserTasteProfile taste = profile(Set.of("Fav Director"), Set.of("Terror"), Set.of("Fav Actor"));

        PersonalMatchScore score = calculator.calculate(theMovie, taste);

        assertThat(score.personalAffinity()).hasValue(5.0);
        assertThat(score.value()).hasValue(31.3); // 80*0.35 + 5*0.65 -> just +3.3 over a no-match
    }

    @Test
    void keepsTheThreeIndividualAffinitySignals() {
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(Actor.of("Fav Actor"), Actor.of("Other")), "2024-01-01",
                List.of(new Rating("TMDB", 7.0, 1000)));
        UserTasteProfile taste = profile(Set.of("Fav Director"), Set.of("Terror"), Set.of("Fav Actor"));

        PersonalAffinitySignals signals = calculator.calculate(theMovie, taste).affinitySignals();

        assertThat(signals).isEqualTo(new PersonalAffinitySignals(100, 100, 50));
    }

    @Test
    void doesNotModifyTheMovieItsRatingsOrTheQualityScoreOriginals() {
        Rating tmdb = new Rating("TMDB", 7.13, 6419);
        List<Rating> ratings = List.of(tmdb);
        Movie theMovie = movie(null, List.of(), List.of(), "2024-01-01", ratings);
        Movie snapshot = movie(null, List.of(), List.of(), "2024-01-01", ratings);

        PersonalMatchScore score = calculator.calculate(theMovie,
                profile(Set.of(), Set.of("Terror"), Set.of()));

        assertThat(theMovie).isEqualTo(snapshot);
        assertThat(theMovie.ratings()).containsExactly(tmdb);
        assertThat(theMovie.ratings().get(0)).isSameAs(tmdb);
        assertThat(tmdb.score()).isEqualTo(7.13); // untouched, still on its own scale
        assertThat(score.qualityScore().normalizedRatings().get(0).original()).isSameAs(tmdb);
    }

    // --- missing-data policy -------------------------------------------------

    @Test
    void movieWithoutRatingsCannotProduceACombinedScore() {
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(), "2024-01-01", List.of());
        UserTasteProfile taste = profile(Set.of("Fav Director"), Set.of("Terror"), Set.of());

        PersonalMatchScore score = calculator.calculate(theMovie, taste);

        assertThat(score.value()).isEmpty();
        assertThat(score.status()).isEqualTo(Status.MISSING_QUALITY);
        assertThat(score.qualityScore().hasValue()).isFalse();
        assertThat(score.personalAffinity()).isPresent(); // affinity was computable
    }

    @Test
    void emptyProfileDoesNotInventAffinityAndCannotProduceACombinedScore() {
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(), "2024-01-01", List.of(new Rating("TMDB", 8.0, 1000)));

        PersonalMatchScore score = calculator.calculate(theMovie, UserTasteProfile.empty());

        assertThat(score.value()).isEmpty();
        assertThat(score.status()).isEqualTo(Status.MISSING_AFFINITY);
        assertThat(score.qualityScore().value()).hasValue(80.0);
        assertThat(score.personalAffinity()).isEmpty();
        assertThat(score.affinitySignals()).isEqualTo(new PersonalAffinitySignals(0, 0, 0));
    }

    @Test
    void noRatingsAndEmptyProfileReportMissingBoth() {
        PersonalMatchScore score = calculator.calculate(
                movie(null, List.of(), List.of(), null, List.of()),
                UserTasteProfile.empty());

        assertThat(score.status()).isEqualTo(Status.MISSING_BOTH);
        assertThat(score.value()).isEmpty();
    }

    @Test
    void aRealAffinityOfZeroIsUsedAndIsNotTreatedAsMissing() {
        Movie theMovie = movie(null, List.of(Genre.of("Terror")), List.of(),
                "2024-01-01", List.of(new Rating("TMDB", 8.0, 1000)));
        UserTasteProfile taste = profile(Set.of("Someone Else"), Set.of("Comedia"), Set.of("Nobody"));

        PersonalMatchScore score = calculator.calculate(theMovie, taste);

        assertThat(score.status()).isEqualTo(Status.COMPLETE);
        assertThat(score.personalAffinity()).hasValue(0.0);
        assertThat(score.value()).hasValue(28.0); // 80 * 0.35 + 0 * 0.65
    }

    // --- estimatedValue: the ranking estimate when quality is missing -------

    @Test
    void estimatedValueEqualsValueWhenQualityIsTrustworthy() {
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(), "2024-01-01", List.of(new Rating("TMDB", 8.0, 1000)));
        UserTasteProfile taste = profile(Set.of("Fav Director"), Set.of("Terror"), Set.of());

        PersonalMatchScore score = calculator.calculate(theMovie, taste);

        assertThat(score.value()).isPresent();
        assertThat(score.estimatedValue()).isEqualTo(score.value());
    }

    @Test
    void estimatedValueGivesTheMissingQualityShareZeroCreditInsteadOfHidingTheCandidate() {
        // no ratings at all -> no trustworthy quality; full director/genre match
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(), "2024-01-01", List.of());
        UserTasteProfile taste = profile(Set.of("Fav Director"), Set.of("Terror"), Set.of());

        PersonalMatchScore score = calculator.calculate(theMovie, taste);

        assertThat(score.value()).isEmpty(); // never a confirmed PERSONAL MATCH SCORE...
        // ...but a real, comparable estimate: director 100, genre 100, actor 0 (no actor
        // preference) -> weighted affinity 90 * 0.65 = 58.5 (no bonuses here)
        assertThat(score.estimatedValue()).hasValue(58.5);
    }

    @Test
    void estimatedValueIsEmptyWhenEvenTheAffinityIsUnknown() {
        Movie theMovie = movie(Director.of("Fav Director"), List.of(Genre.of("Terror")),
                List.of(), "2024-01-01", List.of(new Rating("TMDB", 8.0, 1000)));

        PersonalMatchScore score = calculator.calculate(theMovie, UserTasteProfile.empty());

        assertThat(score.status()).isEqualTo(Status.MISSING_AFFINITY);
        assertThat(score.value()).isEmpty();
        assertThat(score.estimatedValue()).isEmpty();
    }

    @Test
    void bothBonusesStillFoldIntoTheEstimateWhenQualityIsMissing() {
        Movie theMovie = new Movie(1, "tt1", "M", "M", "2026-01-01", 120, "o",
                List.of(Genre.of("Comedia"), Genre.of("Terror")), Director.of("x"),
                List.of(), List.of(Country.of("Nowhere")), null, List.of()); // no ratings
        UserTasteProfile taste = new UserTasteProfile(
                java.util.Map.of("x", 1.0), java.util.Map.of("Terror", 1.0),
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of())
                .withPatterns(List.of(new com.magomez.androidapps.movierec.scoring.TastePattern(
                        "BLACK_COMEDY", "Comedia negra", "d", 0.8,
                        List.of(), List.of(), List.of(), List.of())));

        PersonalMatchScore score = calculator.calculate(theMovie, taste, 0.5);

        assertThat(score.value()).isEmpty();
        assertThat(score.status()).isEqualTo(Status.MISSING_QUALITY);
        assertThat(score.patternBonus()).isGreaterThan(0.0);
        assertThat(score.accoladeBonus()).isEqualTo(10.0); // 20 * 0.5
        // estimate = affinityContribution + patternBonus + accoladeBonus, clamped
        double affinityContribution = score.personalAffinity().getAsDouble() * 0.65;
        assertThat(score.estimatedValue()).hasValue(
                Math.round((affinityContribution + score.patternBonus() + score.accoladeBonus()) * 10.0) / 10.0);
    }

    // --- bounds & rounding -------------------------------------------------

    @Test
    void staysWithinZeroAndOneHundred() {
        Movie zero = movie(null, List.of(Genre.of("Terror")), List.of(), "2024-01-01",
                List.of(new Rating("TMDB", 0.0, 5000)));
        UserTasteProfile noMatch = profile(Set.of("x"), Set.of("y"), Set.of("z"));
        assertThat(calculator.calculate(zero, noMatch).value()).hasValue(0.0);

        Movie hundred = movie(Director.of("Fav"), List.of(Genre.of("Terror")),
                List.of(Actor.of("Fav Actor")), "2024-01-01", List.of(new Rating("TMDB", 10.0, 5000)));
        UserTasteProfile allMatch = profile(Set.of("Fav"), Set.of("Terror"), Set.of("Fav Actor"));
        assertThat(calculator.calculate(hundred, allMatch).value()).hasValue(100.0);
    }

    @Test
    void roundsTheFinalScoreToOneDecimal() {
        // quality 71.3 (7.13*10); director signal 100 -> affinity 55.0
        // 71.3 * 0.35 + 55.0 * 0.65 = 24.955 + 35.75 = 60.705 -> 60.7
        Movie theMovie = movie(Director.of("Fav"), List.of(), List.of(), "2024-01-01",
                List.of(new Rating("TMDB", 7.13, 1000)));
        UserTasteProfile taste = profile(Set.of("Fav"), Set.of(), Set.of());

        assertThat(calculator.calculate(theMovie, taste).value()).hasValue(60.7);
    }

    // --- errors & guards -------------------------------------------------

    @Test
    void unknownRatingSourcePropagates() {
        Movie theMovie = movie(null, List.of(), List.of(), "2024-01-01",
                List.of(new Rating("Letterboxd", 4.0, null)));

        assertThatThrownBy(() -> calculator.calculate(theMovie,
                profile(Set.of(), Set.of("Terror"), Set.of())))
                .isInstanceOf(UnknownRatingSourceException.class)
                .hasMessageContaining("Letterboxd");
    }

    @Test
    void rejectsNullArguments() {
        Movie anyMovie = movie(null, List.of(), List.of(), null, List.of());
        assertThatThrownBy(() -> calculator.calculate(null, UserTasteProfile.empty()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calculator.calculate(anyMovie, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void personalMatchScoreRecordEnforcesItsInvariants() {
        PersonalAffinitySignals signals = new PersonalAffinitySignals(0, 0, 0);

        assertThatThrownBy(() -> new PersonalMatchScore(OptionalDouble.of(101.0),
                QualityScore.of(50.0, List.of()), signals, OptionalDouble.of(50.0)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PersonalMatchScore(OptionalDouble.of(50.0),
                QualityScore.noData(), signals, OptionalDouble.empty()))
                .isInstanceOf(IllegalStateException.class);
    }
}
