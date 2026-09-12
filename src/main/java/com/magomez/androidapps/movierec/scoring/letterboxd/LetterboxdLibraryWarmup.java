package com.magomez.androidapps.movierec.scoring.letterboxd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Builds the {@link LetterboxdLibrary} once, on a background thread, right after the
 * application is ready — so the first real recommendation request does not pay the
 * ~40s of TMDB identification that {@link LetterboxdLibraryLoader#load()} costs cold.
 *
 * <p>It runs <em>after</em> the context is up (the dyno has already bound its port), on
 * its own daemon thread, so it never delays startup. {@code load()}'s own double-checked
 * lock does the rest: a request that arrives mid-warm-up simply blocks until the shared
 * library is ready. A warm-up failure is only logged — the first request rebuilds.
 */
@Component
public class LetterboxdLibraryWarmup {

    private static final Logger log = LoggerFactory.getLogger(LetterboxdLibraryWarmup.class);

    private final LetterboxdLibraryLoader loader;

    public LetterboxdLibraryWarmup(LetterboxdLibraryLoader loader) {
        this.loader = loader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Thread thread = new Thread(this::buildQuietly, "letterboxd-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    private void buildQuietly() {
        try {
            long startedAt = System.currentTimeMillis();
            loader.load();
            log.info("Letterboxd library warmed up in {}s",
                    (System.currentTimeMillis() - startedAt) / 1000);
        } catch (Exception e) {
            log.warn("Letterboxd library warm-up failed (first request will rebuild): {}",
                    e.getMessage());
        }
    }
}
