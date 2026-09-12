package com.magomez.androidapps.movierec.provider.omdb;

import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure transformation from an {@link OmdbResponse} to domain {@link Rating}s. No HTTP,
 * no side effects.
 *
 * <p>Scales are NOT normalized: each rating keeps the source's own scale (IMDb 0-10,
 * Rotten Tomatoes / Metacritic 0-100). Only ratings with a reliable numeric value are
 * emitted, in a fixed order: IMDb, Rotten Tomatoes, Metacritic. A vote count is set
 * only when OMDb actually reports one (IMDb); it is never invented.
 */
public final class OmdbRatingMapper {

    static final String SOURCE_IMDB = "IMDb";
    static final String SOURCE_ROTTEN_TOMATOES = "Rotten Tomatoes";
    static final String SOURCE_METACRITIC = "Metacritic";

    private static final String NOT_AVAILABLE = "N/A";
    private static final Pattern PERCENT = Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)\\s*%");
    private static final Pattern OUT_OF = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*/\\s*\\d+");

    private OmdbRatingMapper() {
    }

    public static List<Rating> toRatings(OmdbResponse omdb) {
        Objects.requireNonNull(omdb, "omdb");
        List<Rating> ratings = new ArrayList<>(3);
        imdb(omdb).ifPresent(ratings::add);
        rottenTomatoes(omdb).ifPresent(ratings::add);
        metacritic(omdb).ifPresent(ratings::add);
        return ratings;
    }

    private static Optional<Rating> imdb(OmdbResponse omdb) {
        Double score = parseDecimal(omdb.imdbRating());
        if (score == null) {
            return Optional.empty();
        }
        return Optional.of(new Rating(SOURCE_IMDB, score, parseVotes(omdb.imdbVotes())));
    }

    private static Optional<Rating> rottenTomatoes(OmdbResponse omdb) {
        return findRatingValue(omdb, SOURCE_ROTTEN_TOMATOES)
                .flatMap(OmdbRatingMapper::parsePercent)
                .map(score -> new Rating(SOURCE_ROTTEN_TOMATOES, score, null));
    }

    private static Optional<Rating> metacritic(OmdbResponse omdb) {
        Optional<Double> score = Optional.ofNullable(parseDecimal(omdb.metascore()));
        if (score.isEmpty()) {
            score = findRatingValue(omdb, SOURCE_METACRITIC).flatMap(OmdbRatingMapper::parseOutOf);
        }
        return score.map(s -> new Rating(SOURCE_METACRITIC, s, null));
    }

    private static Optional<String> findRatingValue(OmdbResponse omdb, String source) {
        return omdb.ratingsOrEmpty().stream()
                .filter(r -> source.equalsIgnoreCase(r.source()))
                .map(OmdbResponse.OmdbRating::value)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private static Double parseDecimal(String raw) {
        if (isMissing(raw)) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            return (Double.isFinite(value) && value >= 0) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseVotes(String raw) {
        if (isMissing(raw)) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim().replace(",", ""));
            return (value >= 0 && value <= Integer.MAX_VALUE) ? (int) value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Optional<Double> parsePercent(String value) {
        Matcher matcher = PERCENT.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        double parsed = Double.parseDouble(matcher.group(1));
        return (parsed >= 0 && parsed <= 100) ? Optional.of(parsed) : Optional.empty();
    }

    private static Optional<Double> parseOutOf(String value) {
        Matcher matcher = OUT_OF.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        double parsed = Double.parseDouble(matcher.group(1));
        return (Double.isFinite(parsed) && parsed >= 0) ? Optional.of(parsed) : Optional.empty();
    }

    private static boolean isMissing(String raw) {
        return raw == null || raw.isBlank() || NOT_AVAILABLE.equalsIgnoreCase(raw.trim());
    }
}
