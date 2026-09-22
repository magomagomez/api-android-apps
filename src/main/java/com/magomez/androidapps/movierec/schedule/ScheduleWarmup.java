package com.magomez.androidapps.movierec.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Fetches the festival's full programme once, on a background thread, right after the
 * application is ready — same pattern as {@code LetterboxdLibraryWarmup} and
 * {@code FestivalLineupWarmup} — so the first real {@code /api/schedule} request doesn't
 * pay that fetch on the caller's thread.
 */
@Component
public class ScheduleWarmup {

    private static final Logger log = LoggerFactory.getLogger(ScheduleWarmup.class);

    private final ScheduleService scheduleService;

    public ScheduleWarmup(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Thread thread = new Thread(this::fetchQuietly, "schedule-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    private void fetchQuietly() {
        try {
            long startedAt = System.currentTimeMillis();
            scheduleService.warmUp();
            log.info("Festival programme warmed up in {}s",
                    (System.currentTimeMillis() - startedAt) / 1000.0);
        } catch (Exception e) {
            log.warn("Festival programme warm-up failed (first request will fetch lazily): {}",
                    e.getMessage());
        }
    }
}
