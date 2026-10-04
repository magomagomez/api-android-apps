package com.magomez.androidapps.jctravels.rides;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.jctravels.rides.dto.ParkFanAttraction;
import com.magomez.androidapps.jctravels.rides.dto.ParkFanSearch;
import com.magomez.androidapps.jctravels.rides.dto.RideForecastDTO;
import com.magomez.androidapps.jctravels.rides.service.RideForecasts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Finding a ride on park.fan and turning its forecast into what the app draws. */
class RideForecastsTest {

    private static final String PARK = "Disney's Animal Kingdom Theme Park";
    private final ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void theRideIsTheOneWithItsExactNameInItsPark() throws IOException {
        ParkFanSearch search = read("parkfan-search-everest.json", ParkFanSearch.class);

        assertThat(RideForecasts.match(search, "Expedition Everest - Legend of the Forbidden Mountain", PARK))
                .isEqualTo("/v1/parks/north-america/united-states/orlando/disneys-animal-kingdom-theme-park/attractions/expedition-everest-legend-of-the-forbidden-mountain");
    }

    @Test
    void namesMatchIgnoringCaseAndSpacing() throws IOException {
        ParkFanSearch search = read("parkfan-search-everest.json", ParkFanSearch.class);

        assertThat(RideForecasts.match(search, " expedition everest -  legend of the forbidden mountain", PARK)).isNotNull();
    }

    @Test
    void noMatchInThatParkMeansNoForecast() throws IOException {
        ParkFanSearch search = read("parkfan-search-everest.json", ParkFanSearch.class);

        assertThat(RideForecasts.match(search, "Expedition Everest - Legend of the Forbidden Mountain", "Magic Kingdom Park")).isNull();
        assertThat(RideForecasts.match(search, "Dinosaur", PARK)).isNull();
    }

    @Test
    void forecastPointsInTimeOrderWithoutEmptyOnes() throws IOException {
        RideForecastDTO forecast = RideForecasts.toDto(read("parkfan-everest.json", ParkFanAttraction.class));

        assertThat(forecast.points()).extracting(RideForecastDTO.Point::time)
                .containsExactly("2026-10-04T13:00:00+00:00", "2026-10-04T13:15:00+00:00");
        assertThat(forecast.points().get(0).minutes()).isEqualTo(20);
        assertThat(forecast.points().get(0).uncertainty()).isEqualTo(15);
        assertThat(forecast.timezone()).isEqualTo("America/New_York");
        assertThat(forecast.source()).isEqualTo("park.fan");
    }

    @Test
    void typicalWaitsByDayOfWeek() throws IOException {
        RideForecastDTO forecast = RideForecasts.toDto(read("parkfan-everest.json", ParkFanAttraction.class));

        assertThat(forecast.typical()).hasSize(2);
        assertThat(forecast.typical().get(1).dayOfWeek()).isEqualTo(6);
        assertThat(forecast.typical().get(1).typical()).isEqualTo(60);
        assertThat(forecast.typical().get(1).busy()).isEqualTo(85);
    }

    @Test
    void nothingKnownIsAnEmptyForecast() {
        RideForecastDTO forecast = RideForecasts.empty();

        assertThat(forecast.points()).isEmpty();
        assertThat(forecast.typical()).isEmpty();
    }

    private <T> T read(String name, Class<T> type) throws IOException {
        try (InputStream json = getClass().getResourceAsStream("/jctravels/" + name)) {
            return mapper.readValue(json, type);
        }
    }
}
