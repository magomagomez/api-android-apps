package com.magomez.androidapps.legacy;

import com.magomez.androidapps.jctravels.parks.dao.ParkDao;
import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.rides.dao.RideDao;
import com.magomez.androidapps.jctravels.rides.dto.ParkInfo;
import com.magomez.androidapps.jctravels.rides.dto.RideFilterRequest;
import com.magomez.androidapps.jctravels.rides.dto.RidesDTO;
import com.magomez.androidapps.jctravels.rides.service.RideQueueService;
import com.magomez.androidapps.jctravels.rides.service.RideService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** A park added from the app has no themeparks.wiki code: its rides list without live waits instead of failing. */
class RidesWithoutLiveTest {

    @Test
    void aParkWithoutCodeListsItsRidesWithoutAskingForLiveData() throws IOException {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        jdbc.nextResult = new Park(9, "SeaWorld", null, 4);
        RideQueueService live = new RideQueueService() {
            @Override
            public ParkInfo getRideQueueTimes(String id) throws IOException {
                throw new IOException("asked themeparks.wiki for entity " + id);
            }
        };

        RidesDTO rides = new RideService(new RideDao(jdbc), live, new ParkDao(jdbc)).getRides(new RideFilterRequest(9));

        assertThat(rides.must()).isEmpty();
        assertThat(rides.maybe()).isEmpty();
    }
}
