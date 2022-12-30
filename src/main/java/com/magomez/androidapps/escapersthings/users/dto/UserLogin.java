package com.magomez.androidapps.escapersthings.users.dto;


import java.io.Serial;
import java.io.Serializable;

public record UserLogin(
        String nombre,
        Integer id
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
