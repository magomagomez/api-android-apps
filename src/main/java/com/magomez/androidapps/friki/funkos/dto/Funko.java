package com.magomez.androidapps.friki.funkos.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record Funko(
        Integer id,
        String name,
        String imagePath,
        Integer number,
        Boolean isWishList,
        List<String> categories) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
