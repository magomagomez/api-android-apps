package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

/** A Lightning Lane queue in themeparks.wiki /live: the next return window and, when paid, its price. */
public record ReturnTime(
        @JsonProperty("state")
        String state,
        @JsonProperty("returnStart")
        String returnStart,
        @JsonProperty("returnEnd")
        String returnEnd,
        @JsonProperty("price")
        Price price
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public record Price(
            @JsonProperty("amount")
            Integer amount,
            @JsonProperty("currency")
            String currency,
            @JsonProperty("formatted")
            String formatted
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
