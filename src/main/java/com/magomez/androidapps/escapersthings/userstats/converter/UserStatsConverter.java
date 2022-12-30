package com.magomez.androidapps.escapersthings.userstats.converter;

import com.magomez.androidapps.escapersthings.userstats.dto.Stats;
import com.magomez.androidapps.escapersthings.userstats.dto.StatsGrades;
import com.magomez.androidapps.escapersthings.userstats.dto.StatsGradesDTO;
import com.magomez.androidapps.escapersthings.userstats.dto.UserStatsDTO;

import java.util.List;

public class UserStatsConverter {

    private UserStatsConverter(){}

    public static List<UserStatsDTO> toDtoList(List<Stats> stats){
        return stats.stream()
                .map(UserStatsConverter::toDto)
                .toList();
    }

    public static UserStatsDTO toDto(Stats stats) {
        return new UserStatsDTO(
                stats.type(),
                stats.total());
    }

    public static List<StatsGradesDTO> gradesToDto(List<StatsGrades> statsGrades){
        return statsGrades.stream()
                .map(UserStatsConverter::toDto)
                .toList();
    }

    private static StatsGradesDTO toDto(StatsGrades statsGrades) {
        return new StatsGradesDTO(
                statsGrades.name(),
                statsGrades.id(),
                statsGrades.userEnigma(),
                statsGrades.userGM(),
                statsGrades.userInmersion(),
                statsGrades.userHorror(),
                statsGrades.userGlobal()
        );
    }
}
