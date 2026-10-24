package com.magomez.androidapps.regalos.items;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static org.assertj.core.api.Assertions.assertThat;

/** What a wish may hold: a title, and optionally a shop link, price, picture and notes. */
class ItemRulesTest {

    static ItemRequest request(String title, String url) {
        return new ItemRequest(title, url, null, null, null, null, null);
    }

    @Test
    void onlyTheTitleIsRequiredAndTheRestDefaults() {
        ValidItem item = ItemRules.valid(request("  Taza de cerámica  ", null));

        assertThat(item).isEqualTo(new ValidItem("Taza de cerámica", null, null, null, null, null, false));
    }

    @Test
    void theStoreComesFromTheLinkWhenMissing() {
        assertThat(ItemRules.valid(request("Sony", "https://www.amazon.es/dp/B09XS7JWHH")).store())
                .isEqualTo("amazon.es");
        assertThat(ItemRules.valid(request("Libro", "http://casadellibro.com/x")).store())
                .isEqualTo("casadellibro.com");
        assertThat(ItemRules.valid(new ItemRequest("Libro", "https://a.com/x", " Mi librería ", null, null, null, null))
                .store()).isEqualTo("Mi librería");
    }

    @Test
    void blankOptionalTextsBecomeNull() {
        ValidItem item = ItemRules.valid(new ItemRequest("Taza", "  ", " ", 2800, "", "   ", true));

        assertThat(item).isEqualTo(new ValidItem("Taza", null, null, 2800, null, null, true));
    }

    @Test
    void rejectsAMissingOrTooLongTitle() {
        assertRegalosError(() -> ItemRules.valid(request("   ", null)), HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> ItemRules.valid(request(null, null)), HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> ItemRules.valid(request("x".repeat(121), null)), HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> ItemRules.valid(null), HttpStatus.BAD_REQUEST, "bad_request");
    }

    @Test
    void linksMustBeHttpOrHttpsWithAHost() {
        for (String link : new String[]{"javascript:alert(1)", "ftp://a.com/x", "file:///etc/passwd", "https://",
                "no es un enlace", "https://a.com/" + "x".repeat(2048)}) {
            assertRegalosError(() -> ItemRules.valid(request("Taza", link)), HttpStatus.BAD_REQUEST, "bad_request");
            assertRegalosError(() -> ItemRules.valid(new ItemRequest("Taza", null, null, null, link, null, null)),
                    HttpStatus.BAD_REQUEST, "bad_request");
        }
    }

    @Test
    void priceIsBetweenZeroAndAMillionEuros() {
        assertThat(ItemRules.valid(new ItemRequest("Taza", null, null, 0, null, null, null)).priceCents()).isZero();
        assertRegalosError(() -> ItemRules.valid(new ItemRequest("Taza", null, null, -1, null, null, null)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> ItemRules.valid(new ItemRequest("Taza", null, null, 100_000_001, null, null, null)),
                HttpStatus.BAD_REQUEST, "bad_request");
    }

    @Test
    void storeAndNotesHaveALimit() {
        assertRegalosError(() -> ItemRules.valid(new ItemRequest("Taza", null, "x".repeat(81), null, null, null, null)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> ItemRules.valid(new ItemRequest("Taza", null, null, null, null, "x".repeat(501), null)),
                HttpStatus.BAD_REQUEST, "bad_request");
    }
}
