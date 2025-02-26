package com.magomez.androidapps.friki.comics.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record Comic(
        Integer id,
        String name,
        Boolean isWishList,
        List<String> categories) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
