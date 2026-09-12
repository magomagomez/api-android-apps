package com.magomez.androidapps.movierec.provider.wikidata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Raw Wikidata SPARQL {@code application/sparql-results+json} payload. Confined to
 * {@code provider/wikidata}: it never leaves the package.
 *
 * <p>Shape: {@code { "results": { "bindings": [ { "<var>": { "value": "..." } } ] } } }.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WikidataSparqlResponse(Results results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Results(List<Map<String, Cell>> bindings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cell(String value) {
    }

    public List<Map<String, Cell>> rows() {
        return results == null || results.bindings() == null ? List.of() : results.bindings();
    }
}
