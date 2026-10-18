package com.magomez.androidapps.jcfit.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YouTubeUrlsTest {

    private static final String ID = "dQw4w9WgXcQ";

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=" + ID,
            "https://youtube.com/watch?v=" + ID + "&t=42s",
            "https://m.youtube.com/watch?feature=share&v=" + ID,
            "http://www.youtube.com/watch?v=" + ID,
            "https://youtu.be/" + ID,
            "https://youtu.be/" + ID + "?si=abc",
            "https://www.youtube.com/shorts/" + ID,
            "https://www.youtube.com/embed/" + ID,
            "  https://youtu.be/" + ID + "  "
    })
    void readsTheVideoIdOfEveryLinkShape(String url) {
        assertEquals(ID, YouTubeUrls.videoId(url).orElseThrow());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "not a url",
            "https://vimeo.com/123456789",
            "https://www.youtube.com/watch?list=PL123",
            "https://www.youtube.com/watch?v=short",
            "https://www.youtube.com/channel/UC1234567890",
            "https://evil.com/watch?v=" + ID,
            "https://youtube.com.evil.com/watch?v=" + ID,
            "javascript:alert(1)",
            "ftp://youtu.be/" + ID
    })
    void rejectsAnythingElse(String url) {
        assertTrue(YouTubeUrls.videoId(url).isEmpty());
    }

    @Test
    void ignoresATrailingSlash() {
        assertEquals(ID, YouTubeUrls.videoId("https://www.youtube.com/shorts/" + ID + "/").orElseThrow());
    }
}
