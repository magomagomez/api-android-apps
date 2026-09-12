package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.scoring.NormalizedRating;
import com.magomez.androidapps.movierec.scoring.QualityScore;
import com.magomez.androidapps.movierec.scoring.QualityScoreCalculator;
import com.magomez.androidapps.movierec.scoring.RatingNormalizer;
import com.magomez.androidapps.movierec.scoring.UnknownRatingSourceException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QualityScoreCalculatorTest {

    private final QualityScoreCalculator calculator =
            new QualityScoreCalculator(new RatingNormalizer());

    private static Movie movieWith(List<Rating> ratings) {
        return new Movie(1, "tt1", "t", "t", null, null, null,
                List.of(), null, List.of(), List.of(), null, ratings);
    }

    private double score(List<Rating> ratings) {
        QualityScore quality = calculator.calculate(movieWith(ratings));
        assertThat(quality.hasValue()).isTrue();
        return quality.value().getAsDouble();
    }

    @Test
    void singleTmdbRating() {
        assertThat(score(List.of(new Rating("TMDB", 7.13, 6419)))).isEqualTo(71.3);
    }

    @Test
    void singleImdbRating() {
        assertThat(score(List.of(new Rating("IMDb", 7.3, 150_234)))).isEqualTo(73.0);
    }

    @Test
    void meanOfTmdbAndImdb() {
        assertThat(score(List.of(
                new Rating("TMDB", 7.0, null),
                new Rating("IMDb", 8.0, null)))).isEqualTo(75.0);
    }

    @Test
    void meanOfAllFourSources() {
        // normalized: 71.3, 73.0, 89.0, 78.0 -> mean 77.825 -> 77.8
        QualityScore quality = calculator.calculate(movieWith(List.of(
                new Rating("TMDB", 7.13, 6419),
                new Rating("IMDb", 7.3, 150_234),
                new Rating("Rotten Tomatoes", 89.0, null),
                new Rating("Metacritic", 78.0, null))));

        assertThat(quality.value().getAsDouble()).isEqualTo(77.8);
        assertThat(quality.normalizedRatings()).extracting(NormalizedRating::score)
                .containsExactly(71.3, 73.0, 89.0, 78.0);
    }

    // --- vote-count confidence guard --------------------------------------

    @Test
    void dropsRatingsWithAKnownVoteCountBelowTheFloor() {
        // an unreleased film with 5 early votes -> that rating is too thin to trust
        QualityScore quality = calculator.calculate(movieWith(List.of(new Rating("TMDB", 9.0, 5))));

        assertThat(quality.hasValue()).isFalse();
        assertThat(quality).isEqualTo(QualityScore.noData());
    }

    @Test
    void keepsRatingsWithoutAVoteCountAndRatingsThatClearTheFloor() {
        // null vote count (RT / Metacritic) is kept; sub-floor TMDB is dropped
        assertThat(score(List.of(
                new Rating("TMDB", 2.0, 4),
                new Rating("Rotten Tomatoes", 90.0, null),
                new Rating("IMDb", 7.0, 10)))) // exactly at the floor -> kept
                .isEqualTo(80.0); // mean(90.0, 70.0)
    }

    // --- corroboration: a lone unsized rating is not trusted alone -----------

    @Test
    void aLoneRatingWithNoKnownVoteCountIsNotTrustedByItself() {
        // Rotten Tomatoes never reports a critic count -- could be 3 reviews or 300
        QualityScore quality = calculator.calculate(
                movieWith(List.of(new Rating("Rotten Tomatoes", 96.0, null))));

        assertThat(quality.hasValue()).isFalse();
        assertThat(quality).isEqualTo(QualityScore.noData());
    }

    @Test
    void twoRatingsWithNoKnownVoteCountCorroborateEachOther() {
        // neither reports a count, but two independent sources agreeing is real corroboration
        assertThat(score(List.of(
                new Rating("Rotten Tomatoes", 90.0, null),
                new Rating("Metacritic", 88.0, null))))
                .isEqualTo(89.0);
    }

    @Test
    void aLoneVolumeVerifiedRatingIsTrustedOnItsOwn() {
        // this one DID clear the vote-count floor, so it doesn't need a second source
        assertThat(score(List.of(new Rating("IMDb", 7.0, 10)))).isEqualTo(70.0);
    }

    @Test
    void noRatingsYieldsNoScore() {
        QualityScore quality = calculator.calculate(movieWith(List.of()));

        assertThat(quality.hasValue()).isFalse();
        assertThat(quality.value().isEmpty()).isTrue();
        assertThat(quality.normalizedRatings()).isEmpty();
        assertThat(quality).isEqualTo(QualityScore.noData());
    }

    @Test
    void doesNotModifyTheMovieOrItsOriginalRatings() {
        Rating tmdb = new Rating("TMDB", 7.13, 6419);
        Rating imdb = new Rating("IMDb", 7.3, 150_234);
        List<Rating> originalRatings = new ArrayList<>(List.of(tmdb, imdb));
        Movie movie = movieWith(originalRatings);

        QualityScore quality = calculator.calculate(movie);

        assertThat(movie.ratings()).containsExactly(tmdb, imdb);
        assertThat(movie.ratings().get(0)).isSameAs(tmdb);
        assertThat(movie.ratings().get(1)).isSameAs(imdb);
        assertThat(tmdb.score()).isEqualTo(7.13); // still on its own 0-10 scale
        assertThat(imdb.score()).isEqualTo(7.3);
        // the normalized ratings keep the untouched originals
        assertThat(quality.normalizedRatings().get(0).original()).isSameAs(tmdb);
        assertThat(quality.normalizedRatings().get(1).original()).isSameAs(imdb);
    }

    @Test
    void unknownRatingSourcePropagatesExplicitly() {
        assertThatThrownBy(() -> calculator.calculate(
                movieWith(List.of(new Rating("Letterboxd", 4.0, null)))))
                .isInstanceOf(UnknownRatingSourceException.class)
                .hasMessageContaining("Letterboxd");
    }

    @Test
    void rejectsANullMovie() {
        assertThatThrownBy(() -> calculator.calculate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void qualityScoreRejectsAValueOutsideZeroTo100() {
        assertThatThrownBy(() -> QualityScore.of(100.1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
