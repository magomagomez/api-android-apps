package com.magomez.androidapps.escapersthings.users.dto;


import java.io.Serial;
import java.io.Serializable;

public record LoginUser(
        String nombre,
        String password
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
