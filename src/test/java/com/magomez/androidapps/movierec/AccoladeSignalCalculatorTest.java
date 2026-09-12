package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.recommendation.AccoladeSignal;
import com.magomez.androidapps.movierec.recommendation.AccoladeSignalCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AccoladeSignalCalculator}: strength is the strongest of award volume, Oscar
 * standing and festival standing — never their sum — stays in {@code [0,1]}, and is
 * deterministic.
 */
class AccoladeSignalCalculatorTest {

    private final AccoladeSignalCalculator calculator = new AccoladeSignalCalculator();

    private static AccoladeReport tally(int oscarWins, int oscarNoms, int wins, int noms) {
        return new AccoladeReport(new AwardsTally(oscarWins, oscarNoms, wins, noms), List.of());
    }

    @Test
    void anEmptyReportIsNoSignal() {
        AccoladeSignal signal = calculator.calculate(AccoladeReport.empty());

        assertThat(signal).isEqualTo(AccoladeSignal.none());
        assertThat(signal.hasRecognition()).isFalse();
    }

    @Test
    void anOscarWinTopsTheSignalOnItsOwn() {
        AccoladeSignal signal = calculator.calculate(tally(1, 0, 1, 0));

        assertThat(signal.strength()).isEqualTo(1.0);
        assertThat(signal.highlights()).contains("Ganó el Óscar");
    }

    @Test
    void anOscarNominationIsAStrongButNotMaximalSignal() {
        assertThat(calculator.calculate(tally(0, 1, 1, 3)).strength()).isEqualTo(0.6);
    }

    @Test
    void awardVolumeSaturates() {
        double few = calculator.calculate(tally(0, 0, 1, 2)).strength();
        double some = calculator.calculate(tally(0, 0, 8, 20)).strength();
        double many = calculator.calculate(tally(0, 0, 30, 70)).strength();

        assertThat(few).isLessThan(some);
        assertThat(some).isLessThan(many);
        assertThat(many).isLessThan(1.0); // volume alone never quite maxes the signal
    }

    @Test
    void aTopFestivalAwardMaxesTheFestivalReading() {
        AccoladeReport report = new AccoladeReport(AwardsTally.empty(), List.of(
                FestivalAchievement.award("Cannes", 2024, "Compétition", "Palme d'Or")));

        AccoladeSignal signal = calculator.calculate(report);

        assertThat(signal.strength()).isEqualTo(1.0);
        assertThat(signal.highlights()).contains("Palme d'Or en Cannes 2024");
    }

    @Test
    void aPlainSelectionIsAModestSignalAndACompetitionSelectionIsHigher() {
        AccoladeReport sidebar = new AccoladeReport(AwardsTally.empty(), List.of(
                FestivalAchievement.selection("Sitges", 2026, "Òrbita")));
        AccoladeReport competition = new AccoladeReport(AwardsTally.empty(), List.of(
                FestivalAchievement.selection("San Sebastián", 2026, "Sección Oficial")));

        assertThat(calculator.calculate(sidebar).strength()).isEqualTo(0.4);
        assertThat(calculator.calculate(competition).strength()).isEqualTo(0.6);
    }

    @Test
    void strengthIsTheMaxOfTheReadingsNotTheSum() {
        // a modest tally AND a modest selection -> still 0.4, not 0.4 + something
        AccoladeReport report = new AccoladeReport(new AwardsTally(0, 0, 1, 1), List.of(
                FestivalAchievement.selection("Fantasia", 2026, "Sélection")));

        assertThat(calculator.calculate(report).strength()).isEqualTo(0.4);
    }

    @Test
    void strengthStaysWithinZeroAndOneAndIsDeterministic() {
        AccoladeReport report = new AccoladeReport(new AwardsTally(2, 3, 60, 140), List.of(
                FestivalAchievement.award("Venezia", 2025, "Concorso", "Leone d'Oro")));

        AccoladeSignal a = calculator.calculate(report);
        AccoladeSignal b = calculator.calculate(report);

        assertThat(a.strength()).isBetween(0.0, 1.0).isEqualTo(b.strength());
        assertThat(a.highlights()).isEqualTo(b.highlights());
    }
}
