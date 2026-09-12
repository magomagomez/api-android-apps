package com.magomez.androidapps.movierec.scoring.letterboxd;

import com.magomez.androidapps.movierec.scoring.UserTasteProfile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The user's Letterboxd data as it is needed during a request: the derived
 * {@link UserTasteProfile} and every movie the user has already seen (by TMDB id, with
 * its title and rating).
 *
 * <p>Both come from the same single pass over the ratings CSV. Temporary and immutable;
 * nothing is persisted.
 *
 * @param profile the weighted taste profile from {@code UserTasteProfileBuilder}
 * @param watched every identified Letterboxd movie; used to exclude already-seen
 *                candidates by id (never by title) and to name similar movies from the
 *                user's history in a recommendation reason
 */
public record LetterboxdLibrary(UserTasteProfile profile, List<WatchedMovie> watched) {

    public LetterboxdLibrary {
        Objects.requireNonNull(profile, "profile");
        watched = watched == null ? List.of() : List.copyOf(watched);
    }

    /** Test / compatibility view: only the ids are known, no titles or ratings. */
    public LetterboxdLibrary(UserTasteProfile profile, Set<Integer> watchedTmdbIds) {
        this(profile, toWatched(watchedTmdbIds));
    }

    public Set<Integer> watchedTmdbIds() {
        return byTmdbId().keySet();
    }

    public boolean hasWatched(Integer tmdbId) {
        return tmdbId != null && byTmdbId().containsKey(tmdbId);
    }

    /** The watched movie with this TMDB id, if the user has seen it. */
    public Optional<WatchedMovie> watchedMovie(Integer tmdbId) {
        return tmdbId == null ? Optional.empty() : Optional.ofNullable(byTmdbId().get(tmdbId));
    }

    private Map<Integer, WatchedMovie> byTmdbId() {
        Map<Integer, WatchedMovie> map = new LinkedHashMap<>();
        for (WatchedMovie w : watched) {
            map.putIfAbsent(w.tmdbId(), w);
        }
        return map;
    }

    private static List<WatchedMovie> toWatched(Set<Integer> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(id -> new WatchedMovie(id, null, 0.0))
                .toList();
    }
}
