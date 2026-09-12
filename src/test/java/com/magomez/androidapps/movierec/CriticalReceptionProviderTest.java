package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.CriticalReception;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.CriticalReceptionProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalReceptionProviderTest {

    private static Movie movie(String title, String releaseDate) {
        return new Movie(null, null, title, title, releaseDate, null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
    }

    /** In-memory {@link CriticalReceptionProvider}: pure domain, no network / HTTP / HTML. */
    private static CriticalReceptionProvider providerReturning(CriticalReception... reception) {
        List<CriticalReception> rows = List.of(reception);
        return new CriticalReceptionProvider() {
            @Override
            public String sourceName() {
                return "FAKE";
            }

            @Override
            public List<CriticalReception> receptionOf(Movie m) {
                return rows;
            }
        };
    }

    @Test
    void isADomainInDomainOutAbstraction() {
        CriticalReception jury = CriticalReception.of("Screen Jury Grid", "Cannes", 2019, 3.2, 4.0, 12);
        CriticalReception trade = CriticalReception.of("IndieWire Critic Poll", "Cannes", 2019, 71, 100, 25);

        CriticalReceptionProvider provider = providerReturning(jury, trade);

        List<CriticalReception> reception = provider.receptionOf(movie("Parasite", "2019-05-30"));

        assertThat(reception).containsExactly(jury, trade);
        assertThat(provider.sourceName()).isEqualTo("FAKE");
    }

    @Test
    void returnsAnEmptyListWhenNothingIsKnown() {
        assertThat(providerReturning().receptionOf(movie("Unknown", "2000-01-01"))).isEmpty();
    }

    @Test
    void makesNoRealCall() {
        // The fake never reaches any network; calling it repeatedly is deterministic.
        CriticalReceptionProvider provider =
                providerReturning(CriticalReception.of("s", "Cannes", 2024, 2.0, 4.0, 5));

        Movie movie = movie("Any", "2024-05-20");
        assertThat(provider.receptionOf(movie)).isEqualTo(provider.receptionOf(movie));
    }
}
