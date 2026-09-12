package com.magomez.androidapps.movierec.provider.cannes;

import com.magomez.androidapps.movierec.model.Movie;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Placeholder {@link CannesArchiveSource}: always returns nothing, never touches the
 * network. Keeps {@link CannesFestivalDataProvider} usable while the real source is
 * pending.
 *
 * <p><b>Why there is no real implementation yet.</b> The official site
 * (festival-cannes.com) offers <em>no</em> public API and no machine-readable dataset
 * for historical selections / palmarès. The data exists only as unstructured HTML:
 * <ul>
 *   <li>film pages at {@code /en/f/{slug}/} — show the edition year and section as
 *       plain text (no JSON-LD, no meta tags);</li>
 *   <li>per-edition archives at {@code /en/archives/{year}/allSelections.html} and
 *       {@code /en/archives/{year}/allAward.html} — HTML lists of the palmarès.</li>
 * </ul>
 *
 * <p>Connecting Cannes therefore still needs: (1) a {@code title + year → official
 * film-page slug} resolver; (2) an HTML parser for the film page (edition + section)
 * and for the per-edition palmarès pages (awards); (3) confirmation that this scraping
 * is permitted (robots / terms). No third-party source is allowed at this stage.
 */
@Component
public class UnavailableCannesArchiveSource implements CannesArchiveSource {

    @Override
    public List<CannesArchiveEntry> lookup(Movie movie) {
        return List.of();
    }
}
