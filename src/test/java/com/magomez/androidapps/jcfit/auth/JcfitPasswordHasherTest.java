package com.magomez.androidapps.jcfit.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Same stored format as escapers v2: pbkdf2$iterations$salt$hash. */
class JcfitPasswordHasherTest {

    private final JcfitPasswordHasher hasher = new JcfitPasswordHasher();

    @Test
    void hashMatchesOnlyItsPassword() {
        String stored = hasher.hash("press-banca");

        assertThat(stored).startsWith("pbkdf2$" + JcfitPasswordHasher.ITERATIONS + "$");
        assertThat(hasher.matches("press-banca", stored)).isTrue();
        assertThat(hasher.matches("press-banco", stored)).isFalse();
    }

    @Test
    void samePasswordGetsADifferentSalt() {
        assertThat(hasher.hash("x")).isNotEqualTo(hasher.hash("x"));
    }

    @Test
    void damagedStoredValuesNeverMatch() {
        assertThat(hasher.matches("x", null)).isFalse();
        assertThat(hasher.matches(null, hasher.hash("x"))).isFalse();
        assertThat(hasher.matches("x", "texto-plano")).isFalse();
        assertThat(hasher.matches("x", "pbkdf2$abc$AAAA$AAAA")).isFalse();
        assertThat(hasher.matches("x", "pbkdf2$1000$%%%$AAAA")).isFalse();
    }
}
