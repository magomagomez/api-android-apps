package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.scoring.pattern.NarrativePattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The deterministic genre/country signatures behind each {@link NarrativePattern}. */
class NarrativePatternTest {

    @Test
    void koreanThrillerNeedsBothKoreaAndATensionGenre() {
        assertThat(NarrativePattern.KOREAN_THRILLER.matchesRaw(
                List.of("Suspense", "Drama"), List.of("Corea del Sur"))).isTrue();
        assertThat(NarrativePattern.KOREAN_THRILLER.matchesRaw(
                List.of("Suspense"), List.of("Estados Unidos de América"))).isFalse();
        assertThat(NarrativePattern.KOREAN_THRILLER.matchesRaw(
                List.of("Comedia"), List.of("Corea del Sur"))).isFalse();
    }

    @Test
    void blackComedyIsComedyCrossedWithHorrorOnly() {
        assertThat(NarrativePattern.BLACK_COMEDY.matchesRaw(List.of("Comedia", "Terror"), List.of())).isTrue();
        assertThat(NarrativePattern.BLACK_COMEDY.matchesRaw(List.of("Comedia", "Horror"), List.of())).isTrue();
        // comedy + crime / thriller / drama no longer count (dramedies, action-comedies)
        assertThat(NarrativePattern.BLACK_COMEDY.matchesRaw(List.of("Comedia", "Crimen"), List.of())).isFalse();
        assertThat(NarrativePattern.BLACK_COMEDY.matchesRaw(List.of("Comedia", "Suspense"), List.of())).isFalse();
        assertThat(NarrativePattern.BLACK_COMEDY.matchesRaw(List.of("Comedia", "Drama"), List.of())).isFalse();
    }

    @Test
    void absurdComedyExcludesAnimationFamilyAdventureAndAction() {
        assertThat(NarrativePattern.ABSURD_COMEDY.matchesRaw(
                List.of("Comedia", "Ciencia ficción"), List.of())).isTrue();
        assertThat(NarrativePattern.ABSURD_COMEDY.matchesRaw(
                List.of("Comedia", "Fantasía"), List.of())).isTrue();
        // Pixar: Comedy + Adventure/Fantasy + Animation/Family -> excluded
        assertThat(NarrativePattern.ABSURD_COMEDY.matchesRaw(
                List.of("Animación", "Comedia", "Aventura", "Fantasía", "Familia"), List.of())).isFalse();
        // superhero comedy: Comedy + Action + Adventure -> excluded (and no sci-fi/fantasy anyway)
        assertThat(NarrativePattern.ABSURD_COMEDY.matchesRaw(
                List.of("Acción", "Aventura", "Comedia", "Ciencia ficción"), List.of())).isFalse();
    }

    @Test
    void uncomfortableCinemaIsDramaPlusTerrorOrSuspenseWithoutComedyOrAction() {
        assertThat(NarrativePattern.UNCOMFORTABLE_CINEMA.matchesRaw(List.of("Drama", "Terror"), List.of())).isTrue();
        assertThat(NarrativePattern.UNCOMFORTABLE_CINEMA.matchesRaw(List.of("Drama", "Suspense"), List.of())).isTrue();
        assertThat(NarrativePattern.UNCOMFORTABLE_CINEMA.matchesRaw(
                List.of("Drama", "Terror", "Comedia"), List.of())).isFalse();
        // action disqualifies it (that is a genre thriller, not "uncomfortable" cinema)
        assertThat(NarrativePattern.UNCOMFORTABLE_CINEMA.matchesRaw(
                List.of("Drama", "Suspense", "Acción"), List.of())).isFalse();
    }

    @Test
    void thoughtProvokingIsDramaPlusPeriodWarOrSciFi() {
        assertThat(NarrativePattern.THOUGHT_PROVOKING.matchesRaw(List.of("Drama", "Historia"), List.of())).isTrue();
        assertThat(NarrativePattern.THOUGHT_PROVOKING.matchesRaw(List.of("Drama", "Bélica"), List.of())).isTrue();
        assertThat(NarrativePattern.THOUGHT_PROVOKING.matchesRaw(List.of("DRAMA", "Ciencia Ficción"), List.of())).isTrue();
        // tightened: drama + misterio (thriller) and lone documental no longer count
        assertThat(NarrativePattern.THOUGHT_PROVOKING.matchesRaw(List.of("Drama", "Misterio"), List.of())).isFalse();
        assertThat(NarrativePattern.THOUGHT_PROVOKING.matchesRaw(List.of("Documental"), List.of())).isFalse();
    }

    @Test
    void triggerGenresInReturnsOnlyThePatternsOwnGenres() {
        assertThat(NarrativePattern.KOREAN_THRILLER.triggerGenresIn(
                List.of("Suspense", "Crimen", "Romance"))).containsExactly("Suspense", "Crimen");
    }

    // --- patternMatch (per-film degree, separate from profile strength) ------

    @Test
    void matchIsZeroWhenTheFilmDoesNotMeetTheSignature() {
        assertThat(NarrativePattern.KOREAN_THRILLER.match(
                List.of("Comedia"), List.of("Corea del Sur"))).isEqualTo(0.0);
        assertThat(NarrativePattern.BLACK_COMEDY.match(
                List.of("Comedia", "Drama"), List.of())).isEqualTo(0.0);
        assertThat(NarrativePattern.PSYCHOLOGICAL_HORROR.match(
                List.of("Comedia", "Romance"), List.of())).isEqualTo(0.0);
    }

    @Test
    void matchIsGradedByHowManyTriggerGenresTheFilmCarries() {
        // KOREAN_THRILLER: base 0.5, span 0.5, cap 2 trigger genres
        double one = NarrativePattern.KOREAN_THRILLER.match(List.of("Crimen"), List.of("Corea del Sur"));
        double two = NarrativePattern.KOREAN_THRILLER.match(
                List.of("Suspense", "Misterio"), List.of("Corea del Sur"));
        assertThat(one).isEqualTo(0.75);
        assertThat(two).isEqualTo(1.0);
        assertThat(two).isGreaterThan(one);

        // genre-pair patterns: base 0.4, span 0.6, cap 3
        double thTwo = NarrativePattern.THOUGHT_PROVOKING.match(List.of("Drama", "Historia"), List.of());
        double thThree = NarrativePattern.THOUGHT_PROVOKING.match(
                List.of("Drama", "Historia", "Bélica"), List.of());
        assertThat(thTwo).isEqualTo(0.8);
        assertThat(thThree).isEqualTo(1.0);
    }

    @Test
    void matchStaysWithinZeroToOne() {
        for (NarrativePattern p : NarrativePattern.values()) {
            double m = p.match(List.of("Comedia", "Terror", "Suspense", "Drama", "Crimen", "Misterio"),
                    List.of("Corea del Sur"));
            assertThat(m).isBetween(0.0, 1.0);
        }
    }
}
