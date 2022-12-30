package com.magomez.androidapps.jctravels.stores.dto;

import java.io.Serial;
import java.io.Serializable;

public record UpdateStore(
        String name,
        Boolean done
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
}
