package com.magomez.androidapps.jcfit.measurements;

import com.magomez.androidapps.jcfit.config.JcfitApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = JcfitApi.BASE_URL + "/measurements", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - progreso físico", description = "Peso, grasa corporal y perímetros")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @Operation(summary = "Every measurement newest first, plus the oldest and the newest")
    @GetMapping
    public MeasurementHistory list() {
        return measurementService.history();
    }

    @Operation(summary = "Add a day's measurement (409 if that day already has one)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Measurement create(@RequestBody MeasurementRequest request) {
        return measurementService.create(request);
    }

    @Operation(summary = "Edit a measurement, its date included")
    @PutMapping("{id}")
    public Measurement update(@PathVariable int id, @RequestBody MeasurementRequest request) {
        return measurementService.update(id, request);
    }

    @Operation(summary = "Delete a measurement with its photo references")
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        measurementService.delete(id);
    }
}
