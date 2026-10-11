package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.auth.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/** PBKDF2WithHmacSHA256 as pbkdf2$<iterations>$<salt>$<hash>, as the v2 contract fixes it. */
class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void storedFormatFollowsTheContract() {
        String[] parts = hasher.hash("caso-cerrado").split("\\$");

        assertThat(parts).hasSize(4);
        assertThat(parts[0]).isEqualTo("pbkdf2");
        assertThat(parts[1]).isEqualTo("210000");
        assertThat(Base64.getDecoder().decode(parts[2])).hasSize(16);
        assertThat(Base64.getDecoder().decode(parts[3])).hasSize(32);
    }

    @Test
    void theRightPasswordMatchesAndAWrongOneDoesNot() {
        String stored = hasher.hash("caso-cerrado");

        assertThat(hasher.matches("caso-cerrado", stored)).isTrue();
        assertThat(hasher.matches("caso-cerrad0", stored)).isFalse();
        assertThat(hasher.matches("", stored)).isFalse();
    }

    @Test
    void everyHashHasItsOwnSalt() {
        assertThat(hasher.hash("misma")).isNotEqualTo(hasher.hash("misma"));
    }

    @Test
    void damagedOrMissingStoredValuesNeverMatch() {
        assertThat(hasher.matches("x", null)).isFalse();
        assertThat(hasher.matches(null, hasher.hash("x"))).isFalse();
        assertThat(hasher.matches("x", "x")).isFalse();
        assertThat(hasher.matches("x", "pbkdf2$abc$AAAA$AAAA")).isFalse();
        assertThat(hasher.matches("x", "pbkdf2$0$AAAA$AAAA")).isFalse();
        assertThat(hasher.matches("x", "pbkdf2$10$no-base64!$AAAA")).isFalse();
        assertThat(hasher.matches("x", "bcrypt$10$AAAA$AAAA")).isFalse();
    }
}
