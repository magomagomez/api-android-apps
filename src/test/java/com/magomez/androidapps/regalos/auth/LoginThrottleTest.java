package com.magomez.androidapps.regalos.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Ten failures in fifteen minutes lock a key (a username or an IP) until the oldest expires. */
class LoginThrottleTest {

    private static final Instant NOW = Instant.parse("2026-10-24T10:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final LoginThrottle throttle = new LoginThrottle(clock);

    @Test
    void nineFailuresStillLetThrough() {
        fail("ip:1.2.3.4", 9);

        assertThatCode(() -> throttle.check("ip:1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void theTenthFailureLocksTheKey() {
        fail("ip:1.2.3.4", 10);

        assertRegalosError(() -> throttle.check("ip:1.2.3.4"), HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts");
    }

    @Test
    void anyLockedKeyOfTheRequestLocksIt() {
        fail("user:cris", 10);

        assertRegalosError(() -> throttle.check("ip:9.9.9.9", "user:cris"),
                HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts");
        assertThatCode(() -> throttle.check("ip:9.9.9.9", "user:alex")).doesNotThrowAnyException();
    }

    @Test
    void failuresOlderThanTheWindowStopCounting() {
        fail("ip:1.2.3.4", 10);
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertThatCode(() -> throttle.check("ip:1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void successForgetsTheFailuresOfThatKey() {
        fail("user:cris", 10);
        throttle.success("user:cris");

        assertThatCode(() -> throttle.check("user:cris")).doesNotThrowAnyException();
    }

    private void fail(String key, int times) {
        for (int i = 0; i < times; i++) {
            throttle.failure(key);
        }
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
