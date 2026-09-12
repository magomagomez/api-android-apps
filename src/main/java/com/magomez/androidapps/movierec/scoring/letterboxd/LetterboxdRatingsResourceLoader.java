package com.magomez.androidapps.movierec.scoring.letterboxd;

import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Loads the project's local Letterboxd ratings CSV from the classpath and turns it into
 * a {@link UserTasteProfile} through the existing pipeline — no HTTP upload, no new
 * logic:
 *
 * <pre>classpath CSV → LetterboxdCsvParser → LetterboxdImportService → UserTasteProfile</pre>
 *
 * <p>The CSV lives at {@code src/main/resources/letterboxd/ratings.csv} (override with
 * the {@code letterboxd.ratings.resource} property). Everything stays in memory; nothing
 * is persisted.
 */
@Service
public class LetterboxdRatingsResourceLoader {

    static final String DEFAULT_RESOURCE = "/letterboxd/ratings.csv";

    private final LetterboxdTasteProfileService letterboxdTasteProfileService;
    private final String resourcePath;

    public LetterboxdRatingsResourceLoader(
            LetterboxdTasteProfileService letterboxdTasteProfileService,
            @Value("${letterboxd.ratings.resource:" + DEFAULT_RESOURCE + "}") String resourcePath) {
        this.letterboxdTasteProfileService = letterboxdTasteProfileService;
        this.resourcePath = resourcePath;
    }

    /**
     * Reads the bundled Letterboxd ratings CSV and builds the user's taste profile.
     *
     * @throws IOException            if the resource cannot be read
     * @throws LetterboxdCsvException if the CSV is not usable
     * @throws IllegalStateException  if the resource is not on the classpath
     */
    public UserTasteProfile loadProfile() throws IOException {
        try (InputStream in = LetterboxdRatingsResourceLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Letterboxd ratings resource not found on the classpath: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return letterboxdTasteProfileService.buildProfile(reader);
            }
        }
    }
}
