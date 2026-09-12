package com.magomez.androidapps.movierec.provider.cannes;

import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.FestivalDataProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * {@link FestivalDataProvider} for the Festival de Cannes.
 *
 * <p>It fetches raw archive rows from a {@link CannesArchiveSource} and maps them to
 * domain {@link FestivalAchievement}s ({@code festival = "Cannes"}): a row with no
 * award name becomes a {@link FestivalAchievement.Type#SELECTION}, a row with an award
 * name becomes an {@link FestivalAchievement.Type#AWARD}.
 *
 * <p>The mapping is complete and tested; the archive source is still a placeholder
 * ({@link UnavailableCannesArchiveSource}), so today this returns an empty list. No
 * {@code festivalScore} is computed here.
 */
@Service
public class CannesFestivalDataProvider implements FestivalDataProvider {

    static final String FESTIVAL = "Cannes";

    private final CannesArchiveSource archiveSource;

    public CannesFestivalDataProvider(CannesArchiveSource archiveSource) {
        this.archiveSource = archiveSource;
    }

    @Override
    public String sourceName() {
        return FESTIVAL;
    }

    @Override
    public List<FestivalAchievement> achievementsOf(Movie movie) {
        Objects.requireNonNull(movie, "movie");
        return archiveSource.lookup(movie).stream()
                .filter(Objects::nonNull)
                .map(CannesFestivalDataProvider::toAchievement)
                .toList();
    }

    private static FestivalAchievement toAchievement(CannesArchiveEntry entry) {
        String awardName = entry.awardName();
        if (awardName == null || awardName.isBlank()) {
            return FestivalAchievement.selection(FESTIVAL, entry.editionYear(), entry.section());
        }
        return FestivalAchievement.award(FESTIVAL, entry.editionYear(), entry.section(), awardName);
    }
}
