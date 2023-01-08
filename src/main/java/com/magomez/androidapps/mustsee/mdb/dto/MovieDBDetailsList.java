package com.magomez.androidapps.mustsee.mdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class MovieDBDetailsList implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("results")
    private List<MovieDBDetails> results;

    public List<MovieDBDetails> getResults() {
        return results;
    }

    public void setResults(List<MovieDBDetails> results) {
        this.results = results;
    }
}
