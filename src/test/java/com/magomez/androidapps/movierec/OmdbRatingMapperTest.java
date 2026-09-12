package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.omdb.OmdbRatingMapper;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse.OmdbRating;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OmdbRatingMapperTest {

    private static OmdbResponse omdb(String imdbRating, String imdbVotes, String metascore,
                                     OmdbRating... ratings) {
        return new OmdbResponse("True", null, imdbRating, imdbVotes, metascore, List.of(ratings));
    }

    @Test
    void mapsImdbRatingAndVoteCount() {
        List<Rating> ratings = OmdbRatingMapper.toRatings(
                omdb("7.3", "150,234", "N/A"));

        assertThat(ratings).containsExactly(new Rating("IMDb", 7.3, 150_234));
    }

    @Test
    void mapsImdbRatingWithoutVoteCountWhenOmdbDoesNotProvideIt() {
        List<Rating> ratings = OmdbRatingMapper.toRatings(omdb("6.1", "N/A", "N/A"));

        assertThat(ratings).containsExactly(new Rating("IMDb", 6.1, null));
    }

    @Test
    void skipsImdbWhenTheRatingIsNotAvailable() {
        assertThat(OmdbRatingMapper.toRatings(omdb("N/A", "1,000", "N/A"))).isEmpty();
    }

    @Test
    void mapsRottenTomatoesFromThePercentageValue() {
        List<Rating> ratings = OmdbRatingMapper.toRatings(
                omdb("N/A", "N/A", "N/A", new OmdbRating("Rotten Tomatoes", "89%")));

        assertThat(ratings).containsExactly(new Rating("Rotten Tomatoes", 89.0, null));
    }

    @Test
    void skipsRottenTomatoesWhenTheValueIsNotAReliableNumber() {
        List<Rating> ratings = OmdbRatingMapper.toRatings(
                omdb("N/A", "N/A", "N/A", new OmdbRating("Rotten Tomatoes", "Fresh")));

        assertThat(ratings).isEmpty();
    }

    @Test
    void mapsMetacriticFromTheMetascoreField() {
        List<Rating> ratings = OmdbRatingMapper.toRatings(omdb("N/A", "N/A", "78"));

        assertThat(ratings).containsExactly(new Rating("Metacritic", 78.0, null));
    }

    @Test
    void mapsMetacriticFromTheRatingsArrayWhenMetascoreIsMissing() {
        List<Rating> ratings = OmdbRatingMapper.toRatings(
                omdb("N/A", "N/A", "N/A", new OmdbRating("Metacritic", "72/100")));

        assertThat(ratings).containsExactly(new Rating("Metacritic", 72.0, null));
    }

    @Test
    void mapsEveryAvailableSourceInAFixedOrder() {
        OmdbResponse response = omdb("7.3", "150,234", "78",
                new OmdbRating("Internet Movie Database", "7.3/10"),
                new OmdbRating("Rotten Tomatoes", "89%"),
                new OmdbRating("Metacritic", "78/100"));

        List<Rating> ratings = OmdbRatingMapper.toRatings(response);

        assertThat(ratings).containsExactly(
                new Rating("IMDb", 7.3, 150_234),
                new Rating("Rotten Tomatoes", 89.0, null),
                new Rating("Metacritic", 78.0, null));
    }

    @Test
    void returnsEmptyWhenNothingIsUsable() {
        OmdbResponse response = omdb("N/A", "N/A", "N/A",
                new OmdbRating("Rotten Tomatoes", "N/A"));

        assertThat(OmdbRatingMapper.toRatings(response)).isEmpty();
    }
}
