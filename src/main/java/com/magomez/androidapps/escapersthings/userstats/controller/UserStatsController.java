package com.magomez.androidapps.escapersthings.userstats.controller;

import com.magomez.androidapps.escapersthings.config.ApiConfig;
import com.magomez.androidapps.escapersthings.userstats.dto.UserGradesDTO;
import com.magomez.androidapps.escapersthings.userstats.dto.UserStatsDTO;
import com.magomez.androidapps.escapersthings.userstats.service.UserStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConfig.BASE_URL + "/user/{userId}")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
public class UserStatsController {

    private final UserStatsService userStatsService;

    @Autowired
    public UserStatsController(final UserStatsService userStatsService){
        this.userStatsService = userStatsService;
    }

    @GetMapping("/stats")
    public List<UserStatsDTO> getGrades(@PathVariable Integer userId) {

        return userStatsService.getStats(userId);
    }

    @GetMapping("/grades")
    public UserGradesDTO getStatsGrades(@PathVariable Integer userId,
                                        @RequestParam(value = "order", required = false) String order) {
        return userStatsService.getUserGrades(userId,order);
    }

    @PostMapping("/escapxeRooms/pendig")
    public void resendPendingEscapes(@PathVariable Integer userId){
        userStatsService.resendPendingEscapeRooms(userId);
    }
}
