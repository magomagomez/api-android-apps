package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibrary;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibraryLoader;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibraryWarmup;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LetterboxdLibraryWarmup}: the ready event triggers exactly one background
 * {@code load()}, and a load failure is swallowed (the first request rebuilds).
 */
class LetterboxdLibraryWarmupTest {

    @Test
    void theReadyEventTriggersASingleBackgroundLoad() throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        AtomicInteger loadCalls = new AtomicInteger();
        LetterboxdLibraryLoader loader = new LetterboxdLibraryLoader(null, null, "unused") {
            @Override
            public LetterboxdLibrary load() {
                loadCalls.incrementAndGet();
                loaded.countDown();
                return new LetterboxdLibrary(UserTasteProfile.empty(), List.of());
            }
        };

        new LetterboxdLibraryWarmup(loader).warmUp();

        assertThat(loaded.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(loadCalls).hasValue(1);
    }

    @Test
    void aWarmUpFailureIsSwallowed() throws Exception {
        CountDownLatch attempted = new CountDownLatch(1);
        LetterboxdLibraryLoader loader = new LetterboxdLibraryLoader(null, null, "unused") {
            @Override
            public LetterboxdLibrary load() throws IOException {
                attempted.countDown();
                throw new IOException("TMDB unavailable at boot");
            }
        };

        new LetterboxdLibraryWarmup(loader).warmUp(); // must not throw

        assertThat(attempted.await(2, TimeUnit.SECONDS)).isTrue();
    }
}
