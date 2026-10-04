package com.magomez.androidapps.jctravels.rides;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.jctravels.rides.converter.RideConverter;
import com.magomez.androidapps.jctravels.rides.dto.ParkInfo;
import com.magomez.androidapps.jctravels.rides.dto.RideDTO;
import com.magomez.androidapps.jctravels.rides.dto.RideInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Single Rider and Lightning Lane from a themeparks.wiki /live response (same mapper settings as RideQueueService). */
class LiveQueuesTest {

    private ParkInfo park;

    @BeforeEach
    void readFixture() throws IOException {
        ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try (InputStream json = getClass().getResourceAsStream("/jctravels/live-animal-kingdom.json")) {
            park = mapper.readValue(json, ParkInfo.class);
        }
    }

    @Test
    void singleRiderAndMultiPassReturnTime() {
        RideDTO ride = apply("64a6915f-a835-4226-ba5c-8389fc4cade3");

        assertThat(ride.getQueueTime()).isEqualTo(45);
        assertThat(ride.getSingleRider()).isEqualTo(10);
        assertThat(ride.getReturnTime().state()).isEqualTo("AVAILABLE");
        assertThat(ride.getReturnTime().start()).isEqualTo("2026-10-04T10:15:00-04:00");
        assertThat(ride.getReturnTime().end()).isEqualTo("2026-10-04T11:15:00-04:00");
        assertThat(ride.getReturnTime().price()).isNull();
        assertThat(ride.getPaidReturnTime()).isNull();
    }

    @Test
    void singlePassKeepsItsPrice() {
        RideDTO ride = apply("24cf863c-b6ba-4826-a056-0b698989cbf7");

        assertThat(ride.getPaidReturnTime().price()).isEqualTo("$19.00");
        assertThat(ride.getPaidReturnTime().start()).isEqualTo("2026-10-04T16:55:00-04:00");
        assertThat(ride.getReturnTime()).isNull();
        assertThat(ride.getSingleRider()).isNull();
    }

    @Test
    void soldOutReturnTimeStillSaysSo() {
        RideDTO ride = apply("8d7ccdb1-a22b-4e26-8dc8-65b1938ed5f0");

        assertThat(ride.getSingleRider()).isNull();
        assertThat(ride.getReturnTime().state()).isEqualTo("FINISHED");
        assertThat(ride.getReturnTime().start()).isNull();
    }

    private RideDTO apply(String code) {
        RideInfo live = park.getLiveData().stream().filter(r -> r.getId().equals(code)).findFirst().orElseThrow();
        RideDTO ride = new RideDTO();
        RideConverter.applyQueues(ride, live.getQueue());
        return ride;
    }
}
