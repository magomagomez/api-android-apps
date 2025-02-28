package com.magomez.androidapps.friki.props.dto;

import java.io.Serial;
import java.io.Serializable;

public record PropFilterRequest(
        String name,
        Boolean wish
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
