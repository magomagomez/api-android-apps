package com.magomez.androidapps.movierec.provider.festival;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.AccoladeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link AccoladeProvider} that answers "which festivals did this film <em>also</em> play"
 * — the signal that actually separates one film in a current-year festival lineup from
 * another (award databases lag months behind; festival programmes are public at once).
 *
 * <p>Configured with a set of festival editions ({@code movierec.festivals}, e.g.
 * {@code "Sundance:2026,SXSW:2026,Cannes:2026"}). Their lineups are fetched once from the
 * {@link FestivalLineupSource} and held for the life of the process (they do not change);
 * a candidate is matched against them by title / original title. Each hit becomes a
 * {@link FestivalAchievement.Type#SELECTION} in that lineup's section.
 *
 * <p>A source failure for one edition is logged and skipped; it never throws.
 */
@Service
public class FestivalLineupAccoladeProvider implements AccoladeProvider {

    private static final String SOURCE_NAME = "festival lineups";
    private static final Logger log = LoggerFactory.getLogger(FestivalLineupAccoladeProvider.class);

    private final FestivalLineupSource lineupSource;
    private final List<FestivalEdition> editions;

    private volatile List<FestivalLineup> lineups;

    public FestivalLineupAccoladeProvider(
            FestivalLineupSource lineupSource,
            @Value("${movierec.festivals:Sundance:2026,SXSW:2026,Cannes:2026,Venice:2026,"
                    + "Fantasia:2026,Berlinale:2026,Locarno:2026,San Sebastian:2026}") String configured) {
        this.lineupSource = lineupSource;
        this.editions = parseEditions(configured);
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public AccoladeReport accoladesOf(Movie movie) {
        if (movie == null) {
            return AccoladeReport.empty();
        }
        List<FestivalAchievement> achievements = new ArrayList<>();
        for (FestivalLineup lineup : loadLineups()) {
            match(lineup, movie).ifPresent(achievements::add);
        }
        return achievements.isEmpty() ? AccoladeReport.empty()
                : new AccoladeReport(AwardsTally.empty(), achievements);
    }

    private static java.util.Optional<FestivalAchievement> match(FestivalLineup lineup, Movie movie) {
        return lineup.lookup(movie.title())
                .or(() -> lineup.lookup(movie.originalTitle()))
                .map(entry -> FestivalAchievement.selection(
                        lineup.festival(), lineup.year(), entry.section()));
    }

    private List<FestivalLineup> loadLineups() {
        List<FestivalLineup> result = lineups;
        if (result == null) {
            synchronized (this) {
                result = lineups;
                if (result == null) {
                    result = fetchAll();
                    lineups = result;
                }
            }
        }
        return result;
    }

    private List<FestivalLineup> fetchAll() {
        List<FestivalLineup> loaded = new ArrayList<>();
        for (FestivalEdition edition : editions) {
            try {
                lineupSource.lineup(edition.festival(), edition.year()).ifPresent(lineup -> {
                    loaded.add(lineup);
                    log.info("Loaded {} {} lineup: {} films",
                            edition.festival(), edition.year(), lineup.entries().size());
                });
            } catch (IOException e) {
                log.warn("Could not load {} {} lineup: {}",
                        edition.festival(), edition.year(), e.getMessage());
            }
        }
        return List.copyOf(loaded);
    }

    private static List<FestivalEdition> parseEditions(String configured) {
        Map<String, FestivalEdition> unique = new LinkedHashMap<>();
        if (configured != null) {
            for (String token : configured.split(",")) {
                String[] parts = token.trim().split(":");
                if (parts.length == 2 && !parts[0].isBlank()) {
                    try {
                        FestivalEdition edition =
                                new FestivalEdition(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                        unique.putIfAbsent(edition.festival() + ":" + edition.year(), edition);
                    } catch (NumberFormatException ignored) {
                        // skip a malformed "Festival:year" token
                    }
                }
            }
        }
        return List.copyOf(unique.values());
    }

    private record FestivalEdition(String festival, int year) {
    }
}
