package com.magomez.androidapps.jcfit.admin;

import com.magomez.androidapps.jcfit.config.JcfitApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = JcfitApi.ADMIN_URL + "/program", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - backoffice programa", description = "Programa semanal activo")
public class AdminProgramController {

    private final AdminProgramService programService;

    public AdminProgramController(AdminProgramService programService) {
        this.programService = programService;
    }

    @Operation(summary = "The active program with its training days")
    @GetMapping
    public AdminProgram get() {
        return programService.get();
    }

    @Operation(summary = "Create the active program, or edit its name and start date")
    @PutMapping
    public AdminProgram save(@RequestBody AdminProgramRequest request) {
        return programService.save(request);
    }

    @Operation(summary = "Replace the whole workout of a day of the week (1 = Monday)")
    @PutMapping("/days/{dayOfWeek}")
    public AdminProgram replaceDay(@PathVariable int dayOfWeek, @RequestBody AdminProgramDayRequest request) {
        return programService.replaceDay(dayOfWeek, request);
    }

    @Operation(summary = "Make a day of the week a rest day")
    @DeleteMapping("/days/{dayOfWeek}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDay(@PathVariable int dayOfWeek) {
        programService.deleteDay(dayOfWeek);
    }
}
