package com.magomez.androidapps.friki.props.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record PropDTO(
        Integer id,
        String name,
        @JsonProperty("wish_list")
        Boolean isWishList,
        List<String> categories
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
