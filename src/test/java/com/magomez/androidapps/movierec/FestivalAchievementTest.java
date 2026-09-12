package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.FestivalAchievement.Type;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FestivalAchievementTest {

    @Test
    void representsASelection() {
        FestivalAchievement selection = FestivalAchievement.selection("Cannes", 2024, "Un Certain Regard");

        assertThat(selection.type()).isEqualTo(Type.SELECTION);
        assertThat(selection.isSelection()).isTrue();
        assertThat(selection.isAward()).isFalse();
        assertThat(selection.festival()).isEqualTo("Cannes");
        assertThat(selection.editionYear()).isEqualTo(2024);
        assertThat(selection.section()).isEqualTo("Un Certain Regard");
        assertThat(selection.awardName()).isNull();
    }

    @Test
    void representsAnAward() {
        FestivalAchievement award = FestivalAchievement.award("Cannes", 2019, "Compétition", "Palme d'Or");

        assertThat(award.type()).isEqualTo(Type.AWARD);
        assertThat(award.isAward()).isTrue();
        assertThat(award.isSelection()).isFalse();
        assertThat(award.awardName()).isEqualTo("Palme d'Or");
        assertThat(award.editionYear()).isEqualTo(2019);
    }

    @Test
    void isImmutableAndValueBased() {
        assertThat(FestivalAchievement.class.isRecord()).isTrue();

        FestivalAchievement a = FestivalAchievement.award("Cannes", 2013, "Compétition", "Palme d'Or");
        FestivalAchievement b = FestivalAchievement.award("Cannes", 2013, "Compétition", "Palme d'Or");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(FestivalAchievement.selection("Cannes", 2013, "Compétition"));
    }

    @Test
    void trimsTextAndTreatsBlankSectionAsUnknown() {
        FestivalAchievement a = new FestivalAchievement("  Cannes  ", 2024, "  Cannes Classics  ",
                Type.AWARD, "  Prix  ");

        assertThat(a.festival()).isEqualTo("Cannes");
        assertThat(a.section()).isEqualTo("Cannes Classics");
        assertThat(a.awardName()).isEqualTo("Prix");
        assertThat(FestivalAchievement.selection("Cannes", 2024, "   ").section()).isNull();
    }

    @Test
    void allowsAnUnknownSection() {
        assertThat(FestivalAchievement.selection("Cannes", 2024, null).section()).isNull();
    }

    @Test
    void rejectsABlankFestival() {
        assertThatThrownBy(() -> FestivalAchievement.selection("  ", 2024, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FestivalAchievement(null, 2024, "x", Type.SELECTION, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnOutOfRangeEditionYear() {
        assertThatThrownBy(() -> FestivalAchievement.selection("Cannes", 1800, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FestivalAchievement.selection("Cannes", 3000, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullType() {
        assertThatThrownBy(() -> new FestivalAchievement("Cannes", 2024, "x", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void anAwardRequiresAnAwardNameAndASelectionRefusesOne() {
        assertThatThrownBy(() -> new FestivalAchievement("Cannes", 2024, "Compétition", Type.AWARD, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FestivalAchievement("Cannes", 2024, "Compétition", Type.AWARD, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FestivalAchievement("Cannes", 2024, "Compétition", Type.SELECTION, "Palme d'Or"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotModelOvationYet() {
        assertThat(Type.values()).containsExactly(Type.SELECTION, Type.AWARD);
    }
}
