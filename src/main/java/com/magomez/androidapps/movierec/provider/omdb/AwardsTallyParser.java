package com.magomez.androidapps.movierec.provider.omdb;

import com.magomez.androidapps.movierec.model.AwardsTally;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure parser for OMDb's free-text {@code "Awards"} line into a domain {@link AwardsTally}.
 * No HTTP, no side effects.
 *
 * <p>Known shapes (case-insensitive):
 * <ul>
 *   <li>{@code "Won 1 Oscar. 9 wins & 23 nominations total"}</li>
 *   <li>{@code "Nominated for 1 Oscar. 5 wins & 8 nominations total"}</li>
 *   <li>{@code "Nominated for 3 BAFTA Awards. 10 wins & 30 nominations total"}</li>
 *   <li>{@code "1 win & 2 nominations"}</li>
 *   <li>{@code "Won 2 Primetime Emmys."} (no totals tail)</li>
 *   <li>{@code "N/A"} / {@code null} / blank &rarr; {@link AwardsTally#empty()}</li>
 * </ul>
 *
 * <p>The {@code "N wins & M nominations"} tail, when present, is authoritative for the
 * totals; the Oscar prefix only breaks the Oscar count out of that total. When there is
 * no tail, a leading {@code "Won N ..."} / {@code "Nominated for N ..."} count is used
 * as the total instead. Anything unrecognised yields {@link AwardsTally#empty()}.
 */
public final class AwardsTallyParser {

    private static final String NOT_AVAILABLE = "N/A";

    private static final Pattern OSCAR_WINS =
            Pattern.compile("won\\s+(\\d+)\\s+oscar", Pattern.CASE_INSENSITIVE);
    private static final Pattern OSCAR_NOMINATIONS =
            Pattern.compile("nominated\\s+for\\s+(\\d+)\\s+oscar", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTALS = Pattern.compile(
            "(\\d+)\\s+wins?\\s*&\\s*(\\d+)\\s+nominations?", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_WON =
            Pattern.compile("won\\s+(\\d+)\\s+[a-z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_NOMINATED =
            Pattern.compile("nominated\\s+for\\s+(\\d+)\\s+[a-z]", Pattern.CASE_INSENSITIVE);

    private AwardsTallyParser() {
    }

    public static AwardsTally parse(String omdbAwards) {
        if (omdbAwards == null || omdbAwards.isBlank()
                || NOT_AVAILABLE.equalsIgnoreCase(omdbAwards.trim())) {
            return AwardsTally.empty();
        }
        String text = omdbAwards.trim();

        int oscarWins = firstInt(OSCAR_WINS, text);
        int oscarNominations = firstInt(OSCAR_NOMINATIONS, text);

        int wins;
        int nominations;
        Matcher totals = TOTALS.matcher(text);
        if (totals.find()) {
            wins = Integer.parseInt(totals.group(1));
            nominations = Integer.parseInt(totals.group(2));
        } else {
            wins = firstInt(LEADING_WON, text);
            nominations = firstInt(LEADING_NOMINATED, text);
        }

        wins = Math.max(wins, oscarWins);
        nominations = Math.max(nominations, oscarNominations);

        if (wins == 0 && nominations == 0) {
            return AwardsTally.empty();
        }
        return new AwardsTally(oscarWins, oscarNominations, wins, nominations);
    }

    private static int firstInt(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
