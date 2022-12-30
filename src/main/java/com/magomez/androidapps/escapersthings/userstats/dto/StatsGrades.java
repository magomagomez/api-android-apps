package com.magomez.androidapps.escapersthings.userstats.dto;


import java.io.Serial;
import java.io.Serializable;

public record StatsGrades(
        String name,
        Integer id,
        Double userEnigma,
        Double userGM,
        Double userInmersion,
        Double userHorror,
        Double userGlobal
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
