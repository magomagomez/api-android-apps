package com.magomez.androidapps.movierec.provider;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.Movie;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Merges every registered {@link AccoladeProvider} into one {@link AccoladeReport} per
 * movie: OMDb gives the aggregate tally, Wikidata / festival archives give the specific
 * {@link com.magomez.androidapps.movierec.model.FestivalAchievement}s.
 *
 * <p>Deliberately not an {@link AccoladeProvider} itself, so Spring's
 * {@code List<AccoladeProvider>} injection here never includes this aggregator. Provider
 * order is Spring's; the merge (see {@link AccoladeReport#mergedWith}) is
 * order-independent for achievements and prefers the richer tally.
 */
@Service
public class AggregatingAccoladeProvider {

    private final List<AccoladeProvider> providers;

    public AggregatingAccoladeProvider(List<AccoladeProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public AccoladeReport accoladesOf(Movie movie) {
        AccoladeReport merged = AccoladeReport.empty();
        for (AccoladeProvider provider : providers) {
            merged = merged.mergedWith(provider.accoladesOf(movie));
        }
        return merged;
    }
}
