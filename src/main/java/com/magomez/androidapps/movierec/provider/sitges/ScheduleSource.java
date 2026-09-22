package com.magomez.androidapps.movierec.provider.sitges;

import com.magomez.androidapps.movierec.model.FilmScreening;

import java.io.IOException;
import java.util.List;

/**
 * Where {@link com.magomez.androidapps.movierec.schedule.ScheduleService} gets the
 * festival's full public screening programme. Kept separate from the service so the HTTP
 * work is isolated and swappable, and the matching logic can be tested with fakes.
 */
public interface ScheduleSource {

    /**
     * @return every screening of the current edition's programme, in source order
     * @throws IOException on a transport failure
     */
    List<FilmScreening> screenings() throws IOException;
}
