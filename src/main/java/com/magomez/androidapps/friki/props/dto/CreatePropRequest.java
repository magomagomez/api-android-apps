package com.magomez.androidapps.friki.props.dto;


import java.io.Serial;
import java.io.Serializable;

public record CreatePropRequest(
        String name,
        String category
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
