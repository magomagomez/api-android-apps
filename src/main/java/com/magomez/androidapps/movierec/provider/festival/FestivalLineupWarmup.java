package com.magomez.androidapps.movierec.provider.festival;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Fetches every configured festival lineup once, on a background thread, right after the
 * application is ready — so the first real recommendation request does not pay the
 * ~1-3s of fetching festival programmes that {@link FestivalLineupAccoladeProvider} would
 * otherwise do lazily, on the caller's thread, the first time it is asked.
 *
 * <p>Mirrors {@code LetterboxdLibraryWarmup}: runs after the context is up (the dyno has
 * already bound its port), on its own daemon thread, so it never delays startup. A
 * warm-up failure is only logged — the first request just fetches lazily instead.
 */
@Component
public class FestivalLineupWarmup {

    private static final Logger log = LoggerFactory.getLogger(FestivalLineupWarmup.class);

    private final FestivalLineupAccoladeProvider provider;

    public FestivalLineupWarmup(FestivalLineupAccoladeProvider provider) {
        this.provider = provider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Thread thread = new Thread(this::fetchQuietly, "festival-lineup-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    private void fetchQuietly() {
        try {
            long startedAt = System.currentTimeMillis();
            provider.warmUp();
            log.info("Festival lineups warmed up in {}s",
                    (System.currentTimeMillis() - startedAt) / 1000.0);
        } catch (Exception e) {
            log.warn("Festival lineup warm-up failed (first request will fetch lazily): {}",
                    e.getMessage());
        }
    }
}
