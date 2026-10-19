package com.magomez.androidapps.jcfit.notifications;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JCFIT_FIREBASE_SERVICE_ACCOUNT: the service account JSON as is or in base64. */
class FcmServiceAccountTest {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";

    @Test
    void readsTheJsonAsIs() {
        FcmServiceAccount account = FcmServiceAccount.parse(FcmTestAccounts.json(TOKEN_URI)).orElseThrow();

        assertThat(account.projectId()).isEqualTo(FcmTestAccounts.PROJECT_ID);
        assertThat(account.clientEmail()).isEqualTo(FcmTestAccounts.CLIENT_EMAIL);
        assertThat(account.tokenUri()).isEqualTo(TOKEN_URI);
        assertThat(account.privateKey().getEncoded()).isEqualTo(FcmTestAccounts.KEYS.getPrivate().getEncoded());
    }

    @Test
    void readsTheJsonInBase64WithOrWithoutLineBreaksAndSpaces() {
        String base64 = FcmTestAccounts.base64(FcmTestAccounts.json(TOKEN_URI));
        String wrapped = base64.replaceAll("(.{76})", "$1\n");

        for (String raw : new String[]{base64, wrapped, "  " + base64 + "\n"}) {
            assertThat(FcmServiceAccount.parse(raw).orElseThrow().projectId()).isEqualTo(FcmTestAccounts.PROJECT_ID);
        }
    }

    @Test
    void withoutTokenUriItUsesGoogles() {
        assertThat(FcmServiceAccount.parse(FcmTestAccounts.json(null)).orElseThrow().tokenUri()).isEqualTo(TOKEN_URI);
    }

    @Test
    void blankIsNotConfigured() {
        assertThat(FcmServiceAccount.parse(null)).isEmpty();
        assertThat(FcmServiceAccount.parse("")).isEqualTo(Optional.empty());
        assertThat(FcmServiceAccount.parse("   ")).isEmpty();
    }

    @Test
    void invalidValuesFailWithoutRepeatingThem() {
        String json = FcmTestAccounts.json(TOKEN_URI);
        String brokenKey = json.replace(FcmTestAccounts.KEY_FRAGMENT, "%%%%" + FcmTestAccounts.KEY_FRAGMENT.substring(4));
        String noProject = json.replace("\"project_id\"", "\"other\"");
        String noEmail = json.replace("\"client_email\"", "\"other\"");
        String noKey = json.replace("\"private_key\"", "\"other\"");
        String notRsa = json.replace(FcmTestAccounts.pem().replace("\n", "\\n"),
                "-----BEGIN PRIVATE KEY-----\\nAAAA\\n-----END PRIVATE KEY-----\\n");

        for (String raw : new String[]{"{not json " + FcmTestAccounts.KEY_FRAGMENT, "%%%" + FcmTestAccounts.KEY_FRAGMENT,
                FcmTestAccounts.base64("[1, 2]"), brokenKey, noProject, noEmail, noKey, notRsa}) {
            assertThatThrownBy(() -> FcmServiceAccount.parse(raw))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasNoCause()
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain(FcmTestAccounts.KEY_FRAGMENT)
                            .doesNotContain("PRIVATE KEY"));
        }
    }

    @Test
    void toStringNeverShowsTheKey() {
        FcmServiceAccount account = FcmServiceAccount.parse(FcmTestAccounts.json(TOKEN_URI)).orElseThrow();

        assertThat(account.toString()).contains(FcmTestAccounts.PROJECT_ID)
                .doesNotContain(FcmTestAccounts.KEY_FRAGMENT)
                .doesNotContain("modulus")
                .doesNotContain("exponent");
    }
}
