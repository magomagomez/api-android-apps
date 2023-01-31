package com.magomez.androidapps.jcwedding.attendant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class RequestAttendantDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("user_ids")
    private List<Integer> id;

    public List<Integer> getId() {
        return id;
    }

    public void setId(List<Integer> id) {
        this.id = id;
    }
}
