package com.magomez.androidapps.jctravels;

import com.magomez.androidapps.jctravels.travels.converter.TravelConverter;
import com.magomez.androidapps.jctravels.travels.dto.Travel;
import com.magomez.androidapps.jctravels.travels.dto.TravelDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Trips can be hidden (the 9999 "USA" template) so the apps stop special-casing an id. */
class TravelConverterTest {

    @Test
    void hiddenFlagReachesTheDto() {
        List<TravelDTO> dtos = TravelConverter.toDtoList(List.of(
                new Travel(1, "East Coast USA", false),
                new Travel(9999, "USA", true)));

        assertThat(dtos).extracting(TravelDTO::hidden).containsExactly(false, true);
    }
}
