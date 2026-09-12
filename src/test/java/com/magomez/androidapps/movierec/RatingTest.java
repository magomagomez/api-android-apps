package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Rating;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class RatingTest {

    @Test
    void holdsSourceScoreAndVoteCount() {
        Rating rating = new Rating("IMDb", 8.1, 1_250_000);

        assertThat(rating.source()).isEqualTo("IMDb");
        assertThat(rating.score()).isEqualTo(8.1);
        assertThat(rating.voteCount()).isEqualTo(1_250_000);
        assertThat(rating.hasVoteCount()).isTrue();
    }

    @Test
    void factoryLeavesVoteCountUnknown() {
        Rating rating = Rating.of("TMDB", 7.4);

        assertThat(rating.voteCount()).isNull();
        assertThat(rating.hasVoteCount()).isFalse();
    }

    @Test
    void trimsSource() {
        assertThat(new Rating("  Rotten Tomatoes  ", 87, 210).source())
                .isEqualTo("Rotten Tomatoes");
    }

    @Test
    void rejectsNullOrBlankSource() {
        assertThatThrownBy(() -> new Rating(null, 8.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source");
        assertThatThrownBy(() -> new Rating("   ", 8.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source");
    }

    @Test
    void rejectsNonFiniteScore() {
        assertThatThrownBy(() -> new Rating("IMDb", Double.NaN, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rating("IMDb", Double.POSITIVE_INFINITY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeScore() {
        assertThatThrownBy(() -> new Rating("IMDb", -0.1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("score");
    }

    @Test
    void rejectsNegativeVoteCount() {
        assertThatThrownBy(() -> new Rating("IMDb", 8.0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voteCount");
    }

    @Test
    void doesNotAssumeAnUpperBoundBecauseScalesDiffer() {
        // 0..10 (IMDb), 0..100 (Metacritic), 0..5 (Letterboxd)... all valid here.
        assertThatCode(() -> new Rating("Metacritic", 100, 40)).doesNotThrowAnyException();
        assertThatCode(() -> new Rating("SomeSource", 12_345.6, null)).doesNotThrowAnyException();
    }

    @Test
    void allowsZeroScoreZeroVotesAndUnknownVotes() {
        assertThatCode(() -> new Rating("TMDB", 0, 0)).doesNotThrowAnyException();
        assertThatCode(() -> new Rating("TMDB", 0, null)).doesNotThrowAnyException();
    }

    @Test
    void isValueBased() {
        assertThat(new Rating("IMDb", 8.1, 100))
                .isEqualTo(new Rating("IMDb", 8.1, 100))
                .isNotEqualTo(new Rating("IMDb", 8.1, 101));
    }
}
