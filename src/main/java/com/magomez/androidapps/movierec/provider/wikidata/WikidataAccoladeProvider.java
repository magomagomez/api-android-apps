package com.magomez.androidapps.movierec.provider.wikidata;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.provider.AccoladeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@link AccoladeProvider} backed by Wikidata's structured award data (SPARQL), looked
 * up strictly by {@link Movie#imdbId()}.
 *
 * <p>Wikidata gives specific award / nomination facts, so this fills
 * {@link AccoladeReport#achievements()} and leaves the tally empty (OMDb owns that):
 * <ul>
 *   <li>an award <em>received</em> &rarr; {@link FestivalAchievement.Type#AWARD};</li>
 *   <li>a festival <em>nomination</em> (the film was in that competition) &rarr; a
 *       {@link FestivalAchievement.Type#SELECTION} in {@code "Competición"};</li>
 *   <li>only <em>festival</em> recognition is kept: the "conferred by" entity must name a
 *       festival, or the award name must be a well-known festival prize. Academy / BAFTA /
 *       Globe categories are dropped — OMDb's tally already counts those, and this tool is
 *       about the festival circuit. Undated or unplaceable facts are dropped too.</li>
 * </ul>
 *
 * <p>API / HTTP error &rarr; logged, {@link AccoladeReport#empty()} (never throws).
 *
 * <p>A film released this year or last is skipped without a network call: award
 * databases lag festival premieres by months to years, so for a brand-new title the
 * query is verified empty far more often than not (real measurement on a 2026 festival
 * lineup: 0 hits) — it would only add latency to every request for essentially no signal.
 * Older candidates (e.g. a retrospective title fed in for comparison) are still queried.
 */
@Service
public class WikidataAccoladeProvider implements AccoladeProvider {

    private static final String SOURCE_NAME = "Wikidata awards";
    private static final String COMPETITION_SECTION = "Competición";
    /** Skip the lookup for a film released this year or last — see the class javadoc. */
    static final int SKIP_IF_RELEASED_WITHIN_YEARS = 1;

    private static final Logger log = LoggerFactory.getLogger(WikidataAccoladeProvider.class);

    /** Festival keywords that may appear inside an award or conferrer label (normalised). */
    private static final Map<String, String> FESTIVAL_KEYWORDS = Map.ofEntries(
            Map.entry("cannes", "Cannes"),
            Map.entry("venice", "Venecia"),
            Map.entry("venezia", "Venecia"),
            Map.entry("berlin", "Berlinale"),
            Map.entry("san sebastian", "San Sebastián"),
            Map.entry("donostia", "San Sebastián"),
            Map.entry("sitges", "Sitges"),
            Map.entry("sundance", "Sundance"),
            Map.entry("locarno", "Locarno"),
            Map.entry("karlovy vary", "Karlovy Vary"),
            Map.entry("toronto international film", "Toronto"),
            Map.entry("fantasia", "Fantasia"),
            Map.entry("south by southwest", "SXSW"),
            Map.entry("rotterdam", "Rotterdam"));

    /** Well-known award names &rarr; the festival that gives them (accent-insensitive keys). */
    private static final Map<String, String> AWARD_TO_FESTIVAL = Map.ofEntries(
            Map.entry("palme d'or", "Cannes"),
            Map.entry("grand prix", "Cannes"),
            Map.entry("prix du jury", "Cannes"),
            Map.entry("camera d'or", "Cannes"),
            Map.entry("golden lion", "Venecia"),
            Map.entry("leone d'oro", "Venecia"),
            Map.entry("silver lion", "Venecia"),
            Map.entry("golden bear", "Berlinale"),
            Map.entry("silver bear", "Berlinale"),
            Map.entry("golden shell", "San Sebastián"),
            Map.entry("concha de oro", "San Sebastián"),
            Map.entry("silver shell", "San Sebastián"),
            Map.entry("concha de plata", "San Sebastián"),
            Map.entry("golden leopard", "Locarno"),
            Map.entry("pardo d'oro", "Locarno"),
            Map.entry("crystal globe", "Karlovy Vary"),
            Map.entry("cheval noir", "Fantasia"),
            Map.entry("melies d'or", "Méliès"),
            Map.entry("melies d'argent", "Méliès"));

    private final WikidataClient client;

    public WikidataAccoladeProvider(WikidataClient client) {
        this.client = client;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public AccoladeReport accoladesOf(Movie movie) {
        if (movie == null || movie.imdbId() == null || movie.imdbId().isBlank()) {
            return AccoladeReport.empty();
        }
        if (isTooRecentForAwardData(movie.releaseDate(), java.time.Year.now().getValue())) {
            return AccoladeReport.empty();
        }
        try {
            List<FestivalAchievement> achievements = new ArrayList<>();
            for (WikidataAward award : client.awardsForImdbId(movie.imdbId())) {
                toAchievement(award)
                        .filter(a -> !achievements.contains(a))
                        .ifPresent(achievements::add);
            }
            return achievements.isEmpty() ? AccoladeReport.empty()
                    : new AccoladeReport(AwardsTally.empty(), achievements);
        } catch (IOException e) {
            log.warn("Wikidata awards lookup failed for imdbId {}: {}",
                    movie.imdbId(), e.getMessage());
            return AccoladeReport.empty();
        }
    }

    private static java.util.Optional<FestivalAchievement> toAchievement(WikidataAward award) {
        String festival = festivalOf(award);
        if (festival == null) {
            return java.util.Optional.empty();
        }
        Integer year = award.year() != null ? award.year() : award.publicationYear();
        if (year == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(award.won()
                ? FestivalAchievement.award(festival, year, null, award.awardLabel())
                : FestivalAchievement.selection(festival, year, COMPETITION_SECTION));
    }

    private static String festivalOf(WikidataAward award) {
        String conferrer = award.conferrerLabel();
        if (conferrer != null && normalize(conferrer).contains("festival")) {
            return conferrer.trim();
        }
        String awardName = normalize(award.awardLabel());
        String byMap = AWARD_TO_FESTIVAL.get(awardName);
        if (byMap != null) {
            return byMap;
        }
        String haystack = awardName + " " + (conferrer == null ? "" : normalize(conferrer));
        return FESTIVAL_KEYWORDS.entrySet().stream()
                .filter(e -> haystack.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /** Pure so the "skip when brand new" rule is testable without depending on wall-clock time. */
    public static boolean isTooRecentForAwardData(String releaseDate, int currentYear) {
        Integer releaseYear = releaseYear(releaseDate);
        return releaseYear != null && releaseYear >= currentYear - SKIP_IF_RELEASED_WITHIN_YEARS;
    }

    private static Integer releaseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT).trim();
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
