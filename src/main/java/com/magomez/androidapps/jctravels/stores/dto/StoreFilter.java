package com.magomez.androidapps.jctravels.stores.dto;

import java.io.Serial;
import java.io.Serializable;

public record StoreFilter(
        Integer outlet
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
