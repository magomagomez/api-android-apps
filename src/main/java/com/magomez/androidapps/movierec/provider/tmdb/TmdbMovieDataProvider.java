package com.magomez.androidapps.movierec.provider.tmdb;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.MovieDataProvider;
import com.magomez.androidapps.movierec.provider.MovieEnricher;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbPersonDetails;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link MovieDataProvider} backed by TMDB.
 *
 * <p>Disambiguation is deterministic and explainable. The first matching search
 * result is never picked arbitrarily. The search is done with English titles (see
 * {@code TmdbClient}) so a Letterboxd title compares cleanly against both the TMDB
 * {@code title} and {@code original_title} (punctuation- and accent-insensitive):
 * <ol>
 *   <li>no results (after an optional retry without the year filter) &rarr; NOT_FOUND;</li>
 *   <li>exactly one result &rarr; IDENTIFIED;</li>
 *   <li>several results: keep those matching the requested year (when given); then keep
 *       those whose title or original title matches exactly;</li>
 *   <li>if that leaves exactly one &rarr; IDENTIFIED;</li>
 *   <li>if it leaves several exact-title matches, keep the one whose audience vote count
 *       clearly dominates (real release vs. duplicates / shorts / fan entries); if that
 *       is decisive &rarr; IDENTIFIED;</li>
 *   <li>if it still leaves several and a director was given, fetch the details of the
 *       remaining candidates and keep those directed by that person; if that leaves
 *       exactly one &rarr; IDENTIFIED;</li>
 *   <li>anything else &rarr; AMBIGUOUS.</li>
 * </ol>
 *
 * <p>It is also a {@link MovieEnricher}: {@link #enrich(Movie)} adds TMDB's
 * {@code vote_average} / {@code vote_count} to an identified movie as a domain
 * {@link Rating}.
 */
@Service
public class TmdbMovieDataProvider implements MovieDataProvider, MovieEnricher {

    private static final String SOURCE_NAME = "TMDB";
    private static final String ENRICHER_NAME = "TMDB rating";
    private static final int MAX_CANDIDATES = 10;
    private static final int MAX_DIRECTOR_LOOKUPS = 5;
    /** Minimum vote count for a candidate to be considered a real, released film. */
    private static final int MIN_DOMINANT_VOTES = 20;
    /** A candidate "dominates" only if its votes are this many times the runner-up's. */
    private static final int VOTE_DOMINANCE_FACTOR = 10;

    private static final Logger log = LoggerFactory.getLogger(TmdbMovieDataProvider.class);

    private final TmdbClient client;
    /**
     * Cache of romanized person names (id &rarr; best Latin display name), so a name
     * credited only in a non-Latin script is shown readably without re-fetching. Person
     * names do not change; this is not persistence of user data.
     */
    private final Map<Integer, String> romanizedNames = new ConcurrentHashMap<>();

    public TmdbMovieDataProvider(TmdbClient client) {
        this.client = client;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public MovieMatch identify(MovieQuery query) throws MovieProviderException {
        try {
            List<TmdbSearchResult> results = client.searchMovies(query.title(), query.year())
                    .resultsOrEmpty();

            if (results.isEmpty() && query.hasYear()) {
                results = client.searchMovies(query.title(), null).resultsOrEmpty();
            }
            if (results.isEmpty()) {
                return MovieMatch.notFound();
            }

            List<TmdbSearchResult> pool = narrow(query, results);
            if (pool.size() == 1) {
                return MovieMatch.identified(loadMovie(pool.get(0)));
            }

            if (query.hasDirector()) {
                Movie byDirector = pickByDirector(query, pool);
                if (byDirector != null) {
                    return MovieMatch.identified(byDirector);
                }
            }

            return MovieMatch.ambiguous(describe(pool));
        } catch (IOException e) {
            throw new MovieProviderException(
                    "TMDB lookup failed for '" + query.title() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String name() {
        return ENRICHER_NAME;
    }

    @Override
    public MovieEnrichment enrich(Movie movie) {
        if (movie == null || movie.tmdbId() == null) {
            return MovieEnrichment.noData(movie);
        }
        try {
            TmdbMovieDetails details = client.movieDetails(movie.tmdbId());
            Optional<Rating> rating = TmdbMovieMapper.toRating(details);
            if (rating.isEmpty()) {
                return MovieEnrichment.noData(movie);
            }
            return MovieEnrichment.applied(movie.withRatings(withRating(movie, rating.get())));
        } catch (IOException e) {
            return MovieEnrichment.failed(movie,
                    "TMDB rating lookup failed for tmdbId " + movie.tmdbId() + ": " + e.getMessage());
        }
    }

    private static List<Rating> withRating(Movie movie, Rating rating) {
        List<Rating> ratings = new ArrayList<>(movie.ratings());
        ratings.add(rating);
        return ratings;
    }

    /**
     * Narrows the search results using year and exact title, without ever selecting a
     * single result by position. Returns the whole (or partially narrowed) pool when
     * no deterministic single match is found.
     */
    private static List<TmdbSearchResult> narrow(MovieQuery query, List<TmdbSearchResult> results) {
        if (results.size() == 1) {
            return results;
        }

        List<TmdbSearchResult> pool = results;
        if (query.hasYear()) {
            List<TmdbSearchResult> byYear = pool.stream()
                    .filter(r -> Objects.equals(query.year(), r.releaseYear()))
                    .toList();
            if (byYear.size() == 1) {
                return byYear;
            }
            if (byYear.size() > 1) {
                pool = byYear;
            }
        }

        List<TmdbSearchResult> byTitle = pool.stream()
                .filter(r -> titleMatches(query.title(), r))
                .toList();
        if (byTitle.size() == 1) {
            return byTitle;
        }
        if (byTitle.isEmpty()) {
            return pool; // no exact title match: do not guess
        }

        // Several exact title (+year) matches. The film the user means is the one whose
        // audience clearly dwarfs the rest (duplicates / shorts / fan entries have ~none).
        return dominantByVotes(byTitle).<List<TmdbSearchResult>>map(List::of).orElse(byTitle);
    }

    private static Optional<TmdbSearchResult> dominantByVotes(List<TmdbSearchResult> candidates) {
        List<TmdbSearchResult> sorted = candidates.stream()
                .sorted(Comparator.comparingInt(TmdbSearchResult::voteCountOrZero).reversed())
                .toList();
        long top = sorted.get(0).voteCountOrZero();
        long runnerUp = sorted.get(1).voteCountOrZero();
        if (top >= MIN_DOMINANT_VOTES && top >= VOTE_DOMINANCE_FACTOR * Math.max(runnerUp, 1)) {
            return Optional.of(sorted.get(0));
        }
        return Optional.empty();
    }

    private Movie pickByDirector(MovieQuery query, List<TmdbSearchResult> pool) throws IOException {
        List<Movie> matches = new ArrayList<>();
        for (TmdbSearchResult candidate : pool.stream().limit(MAX_DIRECTOR_LOOKUPS).toList()) {
            Movie movie = loadMovie(candidate);
            String directorName = movie.director() == null ? null : movie.director().name();
            if (nameMatches(query.director(), directorName)) {
                matches.add(movie);
            }
        }
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private Movie loadMovie(TmdbSearchResult result) throws IOException {
        Movie movie = TmdbMovieMapper.toMovie(client.movieDetails(result.id()));
        return withRomanizedPeople(movie);
    }

    /** Replaces any director / actor name that is only in a non-Latin script with a romanized one. */
    private Movie withRomanizedPeople(Movie movie) {
        Director director = movie.director();
        Director romanizedDirector = director == null ? null
                : new Director(director.tmdbId(), romanize(director.tmdbId(), director.name()));
        List<Actor> romanizedActors = movie.actors().stream()
                .map(a -> new Actor(a.tmdbId(), romanize(a.tmdbId(), a.name())))
                .toList();
        if (Objects.equals(director, romanizedDirector) && romanizedActors.equals(movie.actors())) {
            return movie;
        }
        return movie.withDirectorAndActors(romanizedDirector, romanizedActors);
    }

    private String romanize(Integer personId, String name) {
        if (personId == null || name == null || hasLatinLetters(name)) {
            return name;
        }
        return romanizedNames.computeIfAbsent(personId, id -> {
            try {
                TmdbPersonDetails person = client.personDetails(id);
                if (person != null && hasLatinLetters(person.name())) {
                    return person.name();
                }
                if (person != null) {
                    for (String alias : person.alsoKnownAs()) {
                        if (hasLatinLetters(alias)) {
                            return alias;
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("TMDB person lookup failed for id {}: {}", id, e.getMessage());
            }
            return name; // no Latin spelling available; keep the original
        });
    }

    private static boolean hasLatinLetters(String value) {
        return value != null && value.chars().anyMatch(c -> c < 128 && Character.isLetter(c));
    }

    private static boolean titleMatches(String queryTitle, TmdbSearchResult result) {
        String normalized = normalize(queryTitle);
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.equals(normalize(result.title()))
                || normalized.equals(normalize(result.originalTitle()));
    }

    private static boolean nameMatches(String requested, String actual) {
        if (requested == null || actual == null) {
            return false;
        }
        return normalize(requested).equals(normalize(actual));
    }

    /**
     * Lower-cased, accent-stripped, punctuation-collapsed form for comparison so
     * {@code "Star Wars: Episode II – Attack…"} matches {@code "Star Wars: Episode II - Attack…"}
     * and {@code "Amélie"} matches {@code "Amelie"}.
     */
    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static List<String> describe(List<TmdbSearchResult> results) {
        return results.stream()
                .limit(MAX_CANDIDATES)
                .map(r -> {
                    String title = r.title() != null ? r.title() : r.originalTitle();
                    Integer year = r.releaseYear();
                    return year != null ? title + " (" + year + ")" : String.valueOf(title);
                })
                .toList();
    }
}
