package com.magomez.androidapps.movierec.provider.festival;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.AccoladeProvider;
import com.magomez.androidapps.movierec.support.ExternalCallExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link AccoladeProvider} that answers "which festivals did this film <em>also</em> play"
 * — the signal that actually separates one film in a current-year festival lineup from
 * another (award databases lag months behind; festival programmes are public at once).
 *
 * <p>Configured with a set of festival editions ({@code movierec.festivals}, e.g.
 * {@code "Sundance:2026,SXSW:2026,Cannes:2026"}). Their lineups are fetched once — in
 * parallel, via {@link ExternalCallExecutor} — from the {@link FestivalLineupSource} and
 * held for the life of the process (they do not change); a candidate is matched against
 * them by title / original title. Each hit becomes a {@link FestivalAchievement.Type#SELECTION}
 * in that lineup's section.
 *
 * <p>A source failure for one edition is logged and skipped; it never throws.
 */
@Service
public class FestivalLineupAccoladeProvider implements AccoladeProvider {

    private static final String SOURCE_NAME = "festival lineups";
    private static final Logger log = LoggerFactory.getLogger(FestivalLineupAccoladeProvider.class);

    private final FestivalLineupSource lineupSource;
    private final List<FestivalEdition> editions;
    private final ExternalCallExecutor externalCallExecutor;

    private volatile List<FestivalLineup> lineups;

    @Autowired
    public FestivalLineupAccoladeProvider(
            FestivalLineupSource lineupSource,
            @Value("${movierec.festivals:Sundance:2026,SXSW:2026,Cannes:2026,Venice:2026,"
                    + "Fantasia:2026,Berlinale:2026,Locarno:2026,San Sebastian:2026}") String configured,
            ExternalCallExecutor externalCallExecutor) {
        this.lineupSource = lineupSource;
        this.editions = parseEditions(configured);
        this.externalCallExecutor = externalCallExecutor;
    }

    /** Test constructor: fetches the editions inline, on the caller thread. */
    public FestivalLineupAccoladeProvider(FestivalLineupSource lineupSource, String configured) {
        this(lineupSource, configured, ExternalCallExecutor.sequential());
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    /**
     * Triggers the (cached, once-per-process) lineup fetch eagerly. Used by the startup
     * warm-up so the first real request doesn't pay this cost on the caller's thread.
     */
    public void warmUp() {
        loadLineups();
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

    private static Optional<FestivalAchievement> match(FestivalLineup lineup, Movie movie) {
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
        List<Optional<FestivalLineup>> fetched = externalCallExecutor.map(editions, this::fetchOne);
        return fetched.stream().filter(Optional::isPresent).map(Optional::get).toList();
    }

    private Optional<FestivalLineup> fetchOne(FestivalEdition edition) {
        try {
            Optional<FestivalLineup> lineup = lineupSource.lineup(edition.festival(), edition.year());
            lineup.ifPresent(l -> log.info("Loaded {} {} lineup: {} films",
                    edition.festival(), edition.year(), l.entries().size()));
            return lineup;
        } catch (IOException e) {
            log.warn("Could not load {} {} lineup: {}", edition.festival(), edition.year(), e.getMessage());
            return Optional.empty();
        }
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
