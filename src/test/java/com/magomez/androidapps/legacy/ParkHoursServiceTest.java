package com.magomez.androidapps.legacy;

import com.magomez.androidapps.jctravels.cities.dao.CityDao;
import com.magomez.androidapps.jctravels.parks.dao.ParkDao;
import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.parks.dto.ParkDTO;
import com.magomez.androidapps.jctravels.parks.dto.ScheduleEntry;
import com.magomez.androidapps.jctravels.parks.service.ParkScheduleService;
import com.magomez.androidapps.jctravels.parks.service.ParkService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The parks endpoint carries the opening hours, and a failing schedule never hides the park. */
class ParkHoursServiceTest {

    private final RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();

    @Test
    void aParkComesWithItsHours() {
        String opening = Instant.now().plus(1, ChronoUnit.HOURS).atOffset(ZoneOffset.ofHours(-4)).toString();
        String closing = Instant.now().plus(10, ChronoUnit.HOURS).atOffset(ZoneOffset.ofHours(-4)).toString();
        ParkService parks = service(id -> List.of(new ScheduleEntry("2026-10-04", "OPERATING", null, opening, closing)));
        jdbc.nextResult = new Park(4, "Animal Kingdom", "1c84a229", 4);

        ParkDTO park = parks.getPark(4);

        assertThat(park.hours().opening()).isEqualTo(opening);
        assertThat(park.hours().closing()).isEqualTo(closing);
    }

    @Test
    void aFailingScheduleLeavesTheParkWithoutHours() {
        ParkService parks = service(id -> { throw new IOException("timeout"); });
        jdbc.nextResult = new Park(4, "Animal Kingdom", "1c84a229", 4);

        ParkDTO park = parks.getPark(4);

        assertThat(park.name()).isEqualTo("Animal Kingdom");
        assertThat(park.hours()).isNull();
    }

    private interface Schedules {
        List<ScheduleEntry> of(String queueId) throws IOException;
    }

    private ParkService service(Schedules schedules) {
        ParkScheduleService fake = new ParkScheduleService() {
            @Override
            public List<ScheduleEntry> schedule(String queueId) throws IOException {
                return schedules.of(queueId);
            }
        };
        return new ParkService(new ParkDao(jdbc), new CityDao(jdbc), fake);
    }
}
