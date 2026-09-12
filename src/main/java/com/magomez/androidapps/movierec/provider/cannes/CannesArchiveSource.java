package com.magomez.androidapps.movierec.provider.cannes;

import com.magomez.androidapps.movierec.model.Movie;

import java.util.List;

/**
 * Where {@link CannesFestivalDataProvider} gets its raw Cannes archive rows for a
 * movie. Kept separate from the provider so the (future) HTTP / HTML-scraping work is
 * isolated and swappable, and so the mapping to the domain can be tested with fakes.
 *
 * <p>The real implementation is not written yet — see
 * {@link UnavailableCannesArchiveSource} for what is missing.
 */
public interface CannesArchiveSource {

    /**
     * @return raw Cannes archive rows for {@code movie}, or an empty list when nothing
     *         is known. A network-backed implementation may throw on transport failure;
     *         the placeholder never does.
     */
    List<CannesArchiveEntry> lookup(Movie movie);
}
