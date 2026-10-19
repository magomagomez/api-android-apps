package com.magomez.androidapps.jcfit.progress;

import com.magomez.androidapps.jcfit.config.JcfitApi;
import com.magomez.androidapps.jcfit.measurements.MeasurementHistory;
import com.magomez.androidapps.jcfit.measurements.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = JcfitApi.ADMIN_URL + "/progress", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - backoffice progreso", description = "Progreso de Cris, solo lectura")
public class AdminProgressController {

    private final MeasurementService measurementService;
    private final AdminProgressService progressService;

    public AdminProgressController(MeasurementService measurementService, AdminProgressService progressService) {
        this.measurementService = measurementService;
        this.progressService = progressService;
    }

    @Operation(summary = "Cris's measurements, same as GET /measurements of the app")
    @GetMapping("/measurements")
    public MeasurementHistory measurements() {
        return measurementService.history();
    }

    @Operation(summary = "Cris's latest sessions with their completed sets, newest first (limit 1-100, default 20)")
    @GetMapping("/sessions")
    public List<AdminSessionSummary> sessions(@RequestParam(required = false) Integer limit) {
        return progressService.sessions(limit);
    }
}
