package com.magomez.androidapps.jctravels.rides;

import com.magomez.androidapps.jctravels.rides.converter.RideConverter;
import com.magomez.androidapps.jctravels.rides.dto.Ride;
import com.magomez.androidapps.jctravels.rides.dto.RideDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The park code only names the photo folder; a park added from the app has none. */
class RideConverterTest {

    @Test
    void aKnownParkGivesItsCode() {
        RideDTO ride = RideConverter.toDto(new Ride(1, "Avatar Flight", "flight", true, 4, true, "24cf863c", null));

        assertThat(ride.getPark()).isEqualTo("ANIMAL_KINGDOM");
    }

    @Test
    void aParkAddedFromTheAppHasNoCodeInsteadOfFailing() {
        RideDTO ride = RideConverter.toDto(new Ride(200, "Prova atraccio", null, false, 10, null, null, null));

        assertThat(ride.getPark()).isNull();
        assertThat(ride.getName()).isEqualTo("Prova atraccio");
    }
}
