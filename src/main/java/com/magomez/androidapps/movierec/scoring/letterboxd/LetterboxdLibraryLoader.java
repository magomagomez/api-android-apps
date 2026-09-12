package com.magomez.androidapps.movierec.scoring.letterboxd;

import com.magomez.androidapps.movierec.scoring.RatedMovie;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.UserTasteProfileBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Loads the project's local Letterboxd ratings CSV from the classpath and produces, in a
 * single pass, both things a request needs: the {@link UserTasteProfile} and the set of
 * already-seen TMDB ids ({@link LetterboxdLibrary}).
 *
 * <pre>classpath CSV → LetterboxdImportService (CSV parse + TMDB identify) → UserTasteProfileBuilder</pre>
 *
 * <p>It reuses the existing import and profile-building logic untouched; the only thing
 * added here is collecting the identified movies' TMDB ids alongside the profile. The CSV
 * lives at {@code src/main/resources/letterboxd/ratings.csv} (override with the
 * {@code letterboxd.ratings.resource} property). Everything stays in memory; nothing is
 * persisted.
 *
 * <p>The bundled CSV does not change while the process runs and its identification is
 * deterministic, so the built {@link LetterboxdLibrary} is computed once and reused for
 * the life of the process (the single most expensive part of a recommendation request —
 * ~1.8k TMDB round-trips — otherwise repeats on every call). Still in-memory only, still
 * gone on restart; no profile, list or result is written anywhere.
 */
@Service
public class LetterboxdLibraryLoader {

    static final String DEFAULT_RESOURCE = "/letterboxd/ratings.csv";

    private final LetterboxdImportService letterboxdImportService;
    private final UserTasteProfileBuilder userTasteProfileBuilder;
    private final String resourcePath;

    /** Built once from the immutable bundled CSV, then reused for the life of the process. */
    private volatile LetterboxdLibrary cached;

    public LetterboxdLibraryLoader(
            LetterboxdImportService letterboxdImportService,
            UserTasteProfileBuilder userTasteProfileBuilder,
            @Value("${letterboxd.ratings.resource:" + DEFAULT_RESOURCE + "}") String resourcePath) {
        this.letterboxdImportService = letterboxdImportService;
        this.userTasteProfileBuilder = userTasteProfileBuilder;
        this.resourcePath = resourcePath;
    }

    /**
     * Reads the bundled Letterboxd ratings CSV, identifies each entry against TMDB and
     * builds the taste profile plus the watched-id set.
     *
     * @throws IOException            if the resource cannot be read
     * @throws LetterboxdCsvException if the CSV is not usable
     * @throws IllegalStateException  if the resource is not on the classpath
     */
    public LetterboxdLibrary load() throws IOException {
        LetterboxdLibrary result = cached;
        if (result == null) {
            synchronized (this) {
                result = cached;
                if (result == null) {
                    result = build();
                    cached = result;
                }
            }
        }
        return result;
    }

    private LetterboxdLibrary build() throws IOException {
        try (InputStream in = LetterboxdLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Letterboxd ratings resource not found on the classpath: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                List<RatedMovie> identifiedRatedMovies = letterboxdImportService.importRatings(reader).stream()
                        .filter(LetterboxdImportResult::isIdentified)
                        .map(LetterboxdImportResult::ratedMovie)
                        .toList();

                UserTasteProfile profile = userTasteProfileBuilder.build(identifiedRatedMovies);
                List<WatchedMovie> watched = identifiedRatedMovies.stream()
                        .filter(rated -> rated.movie().tmdbId() != null)
                        .map(rated -> new WatchedMovie(
                                rated.movie().tmdbId(), rated.movie().title(), rated.userScore(),
                                rated.movie().genres().stream()
                                        .map(com.magomez.androidapps.movierec.model.Genre::name)
                                        .toList(),
                                rated.movie().director() == null ? null : rated.movie().director().name()))
                        .toList();

                return new LetterboxdLibrary(profile, watched);
            }
        }
    }
}
