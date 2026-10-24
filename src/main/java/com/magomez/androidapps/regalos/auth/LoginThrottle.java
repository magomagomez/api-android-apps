package com.magomez.androidapps.regalos.auth;

import com.magomez.androidapps.regalos.config.RegalosException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failed logins and registrations per key ("user:cris", "ip:81.0.0.7") in memory:
 * {@link #MAX_FAILURES} inside {@link #WINDOW} lock that key until the oldest one expires.
 * One dyno, so memory is enough; a restart only forgives early.
 */
@Component
public class LoginThrottle {

    static final int MAX_FAILURES = 10;
    static final Duration WINDOW = Duration.ofMinutes(15);
    /** Past this many keys the stale ones are swept, so the map cannot grow without bound. */
    private static final int SWEEP_ABOVE = 10_000;

    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginThrottle(@Qualifier("regalosClock") Clock clock) {
        this.clock = clock;
    }

    /** 429 if any of the keys is locked. */
    public void check(String... keys) {
        Instant since = clock.instant().minus(WINDOW);
        for (String key : keys) {
            Deque<Instant> times = failures.get(key);
            if (times != null && recentCount(times, since) >= MAX_FAILURES) {
                throw new RegalosException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts",
                        "Demasiados intentos fallidos; espera 15 minutos y vuelve a probar");
            }
        }
    }

    public void failure(String... keys) {
        Instant now = clock.instant();
        if (failures.size() > SWEEP_ABOVE) {
            sweep(now.minus(WINDOW));
        }
        for (String key : keys) {
            Deque<Instant> times = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (times) {
                times.addLast(now);
            }
        }
    }

    public void success(String key) {
        failures.remove(key);
    }

    private static int recentCount(Deque<Instant> times, Instant since) {
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(since)) {
                times.removeFirst();
            }
            return times.size();
        }
    }

    private void sweep(Instant since) {
        failures.entrySet().removeIf(entry -> recentCount(entry.getValue(), since) == 0);
    }
}
