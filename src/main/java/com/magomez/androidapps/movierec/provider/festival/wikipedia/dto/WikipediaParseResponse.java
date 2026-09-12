package com.magomez.androidapps.movierec.provider.festival.wikipedia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw MediaWiki {@code action=parse&formatversion=2} payload. Confined to
 * {@code provider/festival/wikipedia}.
 *
 * <p>Success: {@code { "parse": { "title": "...", "text": "<html>" } }}.
 * Missing page: {@code { "error": { "code": "missingtitle", "info": "..." } }}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WikipediaParseResponse(Parse parse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parse(String title, String text) {
    }

    /** The parsed page HTML, or {@code null} when the page does not exist. */
    public String html() {
        return parse == null ? null : parse.text();
    }
}
