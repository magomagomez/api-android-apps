package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.provider.festival.FestivalLineup;
import com.magomez.androidapps.movierec.provider.festival.FestivalLineupAccoladeProvider;
import com.magomez.androidapps.movierec.provider.festival.FestivalLineupSource;
import com.magomez.androidapps.movierec.provider.festival.FestivalLineupWarmup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FestivalLineupWarmup}: the ready event triggers exactly one background
 * {@code warmUp()}, and a failure is swallowed (the first request fetches lazily).
 */
class FestivalLineupWarmupTest {

    @Test
    void theReadyEventTriggersASingleBackgroundWarmUp() throws Exception {
        CountDownLatch fetched = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        FestivalLineupSource source = (festival, year) -> {
            calls.incrementAndGet();
            fetched.countDown();
            return Optional.<FestivalLineup>empty();
        };
        FestivalLineupAccoladeProvider provider =
                new FestivalLineupAccoladeProvider(source, "Sundance:2026");

        new FestivalLineupWarmup(provider).warmUp();

        assertThat(fetched.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void aWarmUpFailureIsSwallowed() throws Exception {
        CountDownLatch attempted = new CountDownLatch(1);
        FestivalLineupSource failing = (festival, year) -> {
            attempted.countDown();
            throw new IOException("wikipedia down at boot");
        };
        FestivalLineupAccoladeProvider provider =
                new FestivalLineupAccoladeProvider(failing, "Sundance:2026");

        new FestivalLineupWarmup(provider).warmUp(); // must not throw

        assertThat(attempted.await(2, TimeUnit.SECONDS)).isTrue();
    }
}
