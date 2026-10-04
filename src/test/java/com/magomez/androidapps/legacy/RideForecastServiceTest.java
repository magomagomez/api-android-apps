package com.magomez.androidapps.legacy;

import com.magomez.androidapps.jctravels.parks.dao.ParkDao;
import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.rides.dao.RideDao;
import com.magomez.androidapps.jctravels.rides.dto.ParkFanAttraction;
import com.magomez.androidapps.jctravels.rides.dto.ParkInfo;
import com.magomez.androidapps.jctravels.rides.dto.Ride;
import com.magomez.androidapps.jctravels.rides.dto.RideForecastDTO;
import com.magomez.androidapps.jctravels.rides.dto.RideInfo;
import com.magomez.androidapps.jctravels.rides.service.ParkFanService;
import com.magomez.androidapps.jctravels.rides.service.RideForecastService;
import com.magomez.androidapps.jctravels.rides.service.RideQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A ride's forecast goes ride → park → official names → park.fan, and any failure is just "no forecast". */
class RideForecastServiceTest {

    private static final String EVEREST = "Expedition Everest - Legend of the Forbidden Mountain";
    private static final String ANIMAL_KINGDOM = "Disney's Animal Kingdom Theme Park";

    /** Answers the ride query with Everest and the park query with Animal Kingdom. */
    private final RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
            return (T) (sql.contains("rides") ? new Ride(1, "Everest", "everest", false, 4, true, "64a6915f", null) : new Park(4, "Animal Kingdom", "1c84a229", 4));
        }
    };
    private final List<String> asked = new ArrayList<>();

    @Test
    void theForecastOfTheRideWithItsOfficialName() {
        RideForecastDTO forecast = service(new FakeParkFan(false)).forecast(1);

        assertThat(asked).containsExactly(EVEREST + " @ " + ANIMAL_KINGDOM, "/v1/everest");
        assertThat(forecast.points()).hasSize(1);
    }

    @Test
    void parkFanDownMeansNoForecastNotAnError() {
        RideForecastDTO forecast = service(new FakeParkFan(true)).forecast(1);

        assertThat(forecast.points()).isEmpty();
        assertThat(forecast.source()).isEqualTo("park.fan");
    }

    private RideForecastService service(ParkFanService parkFan) {
        RideQueueService live = new RideQueueService() {
            @Override
            public ParkInfo getRideQueueTimes(String id) {
                RideInfo ride = new RideInfo();
                ride.setId("64a6915f");
                ride.setName(EVEREST);
                ParkInfo park = new ParkInfo();
                park.setName(ANIMAL_KINGDOM);
                park.setLiveData(List.of(ride));
                return park;
            }
        };
        return new RideForecastService(new RideDao(jdbc), new ParkDao(jdbc), live, parkFan);
    }

    private class FakeParkFan extends ParkFanService {
        private final boolean down;

        FakeParkFan(boolean down) {
            this.down = down;
        }

        @Override
        public String attractionUrl(String rideName, String parkName) throws IOException {
            asked.add(rideName + " @ " + parkName);
            if (down) {
                throw new IOException("timeout");
            }
            return "/v1/everest";
        }

        @Override
        public ParkFanAttraction attraction(String url) {
            asked.add(url);
            return new ParkFanAttraction(null, List.of(new ParkFanAttraction.Prediction("2026-10-04T13:00:00+00:00", 20, 15)), null);
        }
    }
}
