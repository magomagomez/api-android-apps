package com.magomez.androidapps.jcfit.admin;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = JcfitApi.ADMIN_URL + "/exercises", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - backoffice ejercicios", description = "Catálogo de ejercicios")
public class AdminExerciseController {

    private final AdminExerciseService exerciseService;

    public AdminExerciseController(AdminExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @Operation(summary = "Every exercise by name; the archived only when asked for")
    @GetMapping
    public List<AdminExercise> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return exerciseService.list(includeArchived);
    }

    @Operation(summary = "Add an exercise")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminExercise create(@RequestBody AdminExerciseRequest request) {
        return exerciseService.create(request);
    }

    @Operation(summary = "Edit an exercise, archived included")
    @PutMapping("{exerciseId}")
    public AdminExercise update(@PathVariable int exerciseId, @RequestBody AdminExerciseRequest request) {
        return exerciseService.update(exerciseId, request);
    }

    @Operation(summary = "Archive an exercise (it is never deleted)")
    @DeleteMapping("{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable int exerciseId) {
        exerciseService.archive(exerciseId);
    }
}
