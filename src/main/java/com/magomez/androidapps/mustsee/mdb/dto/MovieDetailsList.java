package com.magomez.androidapps.mustsee.mdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class MovieDetailsList implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("page")
    private String page;
    @JsonProperty("results")
    private List<MovieDetails> results;

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public List<MovieDetails> getResults() {
        return results;
    }

    public void setResults(List<MovieDetails> results) {
        this.results = results;
    }
}
