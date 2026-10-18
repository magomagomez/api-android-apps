package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.JcfitApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(value = JcfitApi.BASE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - entrenos", description = "Semana, entreno del día e historial por ejercicio")
public class TrainingController {

    private final WeekService weekService;
    private final WorkoutDayService dayService;
    private final ExerciseHistoryService historyService;

    public TrainingController(WeekService weekService, WorkoutDayService dayService,
                              ExerciseHistoryService historyService) {
        this.weekService = weekService;
        this.dayService = dayService;
        this.historyService = historyService;
    }

    @Operation(summary = "Monday to Sunday of the week that contains the date, with each day's status")
    @GetMapping("weeks/{date}")
    public WeekView week(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return weekService.week(date);
    }

    @Operation(summary = "A program day on a date: exercises, previous sets and logged sets")
    @GetMapping("days/{programDayId}")
    public WorkoutDayView day(@PathVariable int programDayId,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dayService.day(programDayId, date);
    }

    @Operation(summary = "Completed sets of an exercise, newest session first (limit 1-100, default 20)")
    @GetMapping("exercises/{exerciseId}/history")
    public ExerciseHistory history(@PathVariable int exerciseId, @RequestParam(required = false) Integer limit) {
        return historyService.history(exerciseId, limit);
    }
}
