package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.scoring.NormalizedRating;
import com.magomez.androidapps.movierec.scoring.RatingNormalizer;
import com.magomez.androidapps.movierec.scoring.UnknownRatingSourceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingNormalizerTest {

    private final RatingNormalizer normalizer = new RatingNormalizer();

    private double normalize(String source, double score) {
        return normalizer.normalize(new Rating(source, score, null)).score();
    }

    @Test
    void normalizesTmdbFrom0To10() {
        assertThat(normalize("TMDB", 7.13)).isEqualTo(71.3);
    }

    @Test
    void normalizesImdbFrom0To10() {
        assertThat(normalize("IMDb", 7.3)).isEqualTo(73.0);
    }

    @Test
    void keepsRottenTomatoesOnItsOwn0To100Scale() {
        assertThat(normalize("Rotten Tomatoes", 89)).isEqualTo(89.0);
    }

    @Test
    void keepsMetacriticOnItsOwn0To100Scale() {
        assertThat(normalize("Metacritic", 78)).isEqualTo(78.0);
    }

    @Test
    void handlesScaleBoundaries() {
        assertThat(normalize("TMDB", 0)).isEqualTo(0.0);
        assertThat(normalize("TMDB", 10)).isEqualTo(100.0);
        assertThat(normalize("IMDb", 0)).isEqualTo(0.0);
        assertThat(normalize("IMDb", 10)).isEqualTo(100.0);
        assertThat(normalize("Rotten Tomatoes", 0)).isEqualTo(0.0);
        assertThat(normalize("Rotten Tomatoes", 100)).isEqualTo(100.0);
        assertThat(normalize("Metacritic", 0)).isEqualTo(0.0);
        assertThat(normalize("Metacritic", 100)).isEqualTo(100.0);
    }

    @Test
    void keepsTheOriginalRatingAndItsScaleUntouched() {
        Rating original = new Rating("TMDB", 7.13, 6419);

        NormalizedRating normalized = normalizer.normalize(original);

        assertThat(normalized.original()).isSameAs(original);
        assertThat(normalized.original().score()).isEqualTo(7.13); // still on the 0-10 scale
        assertThat(normalized.original().voteCount()).isEqualTo(6419);
        assertThat(normalized.source()).isEqualTo("TMDB");
        assertThat(normalized.score()).isEqualTo(71.3);
    }

    @Test
    void rejectsUnknownSourcesExplicitly() {
        assertThatThrownBy(() -> normalizer.normalize(new Rating("Letterboxd", 4.0, null)))
                .isInstanceOf(UnknownRatingSourceException.class)
                .hasMessageContaining("Letterboxd");
    }

    @Test
    void alwaysProducesAScoreWithinZeroTo100EvenWhenTheOriginalExceedsItsScale() {
        assertThat(normalize("IMDb", 11.0)).isEqualTo(100.0);
        assertThat(normalize("Metacritic", 250.0)).isEqualTo(100.0);
    }

    @Test
    void rejectsANullRating() {
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalizedRatingRejectsValuesOutsideZeroTo100() {
        Rating any = new Rating("TMDB", 5.0, null);
        assertThatThrownBy(() -> new NormalizedRating(any, 101.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NormalizedRating(any, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NormalizedRating(any, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
