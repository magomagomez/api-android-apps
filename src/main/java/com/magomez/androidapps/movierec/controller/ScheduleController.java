package com.magomez.androidapps.movierec.controller;

import com.magomez.androidapps.movierec.api.ScheduleApiMapper;
import com.magomez.androidapps.movierec.api.ScheduleRequest;
import com.magomez.androidapps.movierec.api.ScheduleResponse;
import com.magomez.androidapps.movierec.schedule.ScheduleResult;
import com.magomez.androidapps.movierec.schedule.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

/**
 * Turns a handful of picked titles (typically a TOP N from {@code /api/recommendations})
 * into a day-by-day festival calendar: which days they screen, at what time, and whether
 * that slot is realistically attendable (see {@link com.magomez.androidapps.movierec.schedule.SchedulePriority}).
 *
 * <p>Controller &rarr; Service &rarr; Provider, same as {@link RecommendationController}.
 * The festival programme warms up in the background at startup ({@code ScheduleWarmup});
 * a request that lands mid-warm-up fails fast with a plain {@code 503} instead of blocking.
 */
@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ScheduleResponse schedule(@RequestBody ScheduleRequest request) {
        if (!scheduleService.isReady()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Todavía estamos cargando el programa del festival (arranque en curso). "
                            + "Reintenta en unos segundos.");
        }
        List<String> titles = ScheduleApiMapper.toTitles(request);
        try {
            ScheduleResult result = scheduleService.buildSchedule(titles);
            return ScheduleApiMapper.toResponse(result);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "No se pudo consultar el programa del festival", e);
        }
    }
}
