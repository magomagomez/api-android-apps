package com.magomez.androidapps.escapersthings.v2.stats;

import com.magomez.androidapps.escapersthings.v2.auth.CurrentUser;
import com.magomez.androidapps.escapersthings.v2.config.ApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Escapers v2 - estadísticas", description = "Mis números")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @Operation(summary = "My totals, rooms by category, tops and worst, my average and the group's")
    @GetMapping(ApiConfig.BASE_URL + "/me/stats")
    public Stats stats(@Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user) {
        return statsService.stats(user.id());
    }
}
