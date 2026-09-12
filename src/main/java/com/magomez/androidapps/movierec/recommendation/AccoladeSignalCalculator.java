package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.FestivalAchievement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Turns a raw {@link AccoladeReport} into an {@link AccoladeSignal}: a deterministic
 * {@code [0,1]} strength and human-readable highlights. Pure, no external calls, no LLM.
 *
 * <p><b>strength</b> is the <em>strongest</em> of three readings, never their sum — a
 * Palme d'Or does not need twenty more trophies to reach 1.0:
 * <pre>
 *   volumeScore   = 1 - exp( -(wins + 0.4*nominations) / 8 )      // saturating
 *   oscarScore    = 1.0 win / 0.6 nomination / 0
 *   festivalScore = 1.0 top festival award / 0.75 other award
 *                 / 0.6 competition selection / 0.4 other selection
 *   strength      = round2( max(volumeScore, oscarScore, festivalScore) )
 * </pre>
 */
@Component
public class AccoladeSignalCalculator {

    private static final double VOLUME_K = 25.0;
    private static final double NOMINATION_WEIGHT = 0.4;

    private static final double TOP_FESTIVAL_AWARD = 1.0;
    private static final double OTHER_AWARD = 0.75;
    private static final double COMPETITION_SELECTION = 0.6;
    private static final double OTHER_SELECTION = 0.4;

    /** Award names that top out the festival reading, matched accent-insensitively. */
    private static final Set<String> TOP_AWARDS = Set.of(
            "palme d'or", "grand prix", "golden lion", "leone d'oro", "leon de oro",
            "golden bear", "goldener bar", "oso de oro", "concha de oro", "golden shell",
            "cheval noir", "grand jury prize", "melies d'or", "melies de oro",
            "audience award", "premio del publico", "critics' week grand prize");

    /** Section names that count as a competitive selection, matched accent-insensitively. */
    private static final Set<String> COMPETITION_SECTIONS = Set.of(
            "competition", "competition officielle", "in competition", "official competition",
            "seleccion oficial", "seccion oficial", "official selection", "concurso",
            "competencia", "concorso", "competicion");

    public AccoladeSignal calculate(AccoladeReport report) {
        Objects.requireNonNull(report, "report");
        if (report.isEmpty()) {
            return AccoladeSignal.none();
        }

        AwardsTally tally = report.tally();
        double volumeScore = 1.0 - Math.exp(
                -(tally.wins() + NOMINATION_WEIGHT * tally.nominations()) / VOLUME_K);
        double oscarScore = tally.oscarWins() > 0 ? 1.0 : tally.oscarNominations() > 0 ? 0.6 : 0.0;
        double festivalScore = report.achievements().stream()
                .mapToDouble(AccoladeSignalCalculator::score)
                .max()
                .orElse(0.0);

        double strength = round2(Math.max(volumeScore, Math.max(oscarScore, festivalScore)));
        return new AccoladeSignal(strength, highlights(report, volumeScore), tally, report.achievements());
    }

    private static double score(FestivalAchievement achievement) {
        if (achievement.isAward()) {
            return isTopAward(achievement.awardName()) ? TOP_FESTIVAL_AWARD : OTHER_AWARD;
        }
        return isCompetitionSection(achievement.section())
                ? COMPETITION_SELECTION : OTHER_SELECTION;
    }

    private static boolean isTopAward(String awardName) {
        return awardName != null && TOP_AWARDS.contains(normalize(awardName));
    }

    private static boolean isCompetitionSection(String section) {
        return section != null && COMPETITION_SECTIONS.contains(normalize(section));
    }

    /** Qualitative, deterministic — never a raw count of "wins"/"nominations". */
    private static List<String> highlights(AccoladeReport report, double volumeScore) {
        List<String> lines = new ArrayList<>();
        AwardsTally tally = report.tally();
        if (tally.oscarWins() > 0) {
            lines.add(tally.oscarWins() == 1 ? "Ganó el Óscar"
                    : "Ganó varios Óscars");
        } else if (tally.oscarNominations() > 0) {
            lines.add("Nominada al Óscar");
        }
        for (FestivalAchievement achievement : report.achievements()) {
            lines.add(describe(achievement));
        }
        if (volumeScore >= 0.6) {
            lines.add("Ha cosechado numerosos premios y nominaciones en distintos certámenes.");
        } else if (volumeScore >= 0.25) {
            lines.add("Cuenta con varios premios y nominaciones a sus espaldas.");
        } else if (tally.wins() > 0 || tally.nominations() > 0) {
            lines.add("Ha recibido algún reconocimiento en premios de la industria.");
        }
        return lines;
    }

    private static String describe(FestivalAchievement achievement) {
        String where = achievement.festival() + " " + achievement.editionYear();
        if (achievement.isAward()) {
            return achievement.awardName() + " en " + where;
        }
        String section = achievement.section() == null ? "" : " (" + achievement.section() + ")";
        return "Selección de " + where + section;
    }

    private static String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT).trim();
        return java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
