package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.auth.PasswordHashBackfill;
import com.magomez.androidapps.escapersthings.v2.auth.PasswordHasher;
import com.magomez.androidapps.escapersthings.v2.auth.UserDao;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;

import static com.magomez.androidapps.escapersthings.v2.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** The start-up job that hashes the legacy plain-text passwords, once. */
class PasswordHashBackfillTest {

    private static final String WITHOUT_HASH = "WHERE password_hash IS NULL ORDER BY id";
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesOnlyTheUsersWithoutHashAndNeverOverwritesOne() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate()
                .rows(WITHOUT_HASH, row("id", 4, "password", "sara1234"));

        run(jdbc);

        RecordingJdbcTemplate.Call fill = jdbc.last();
        assertThat(fill.sql()).isEqualTo("UPDATE users SET password_hash = ? WHERE id = ? AND password_hash IS NULL");
        assertThat(hasher.matches("sara1234", (String) fill.args()[0])).isTrue();
        assertThat(fill.args()[1]).isEqualTo(4);
    }

    @Test
    void secondRunFindsNothingAndWritesNothing() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();

        run(jdbc);

        assertThat(jdbc.calls).hasSize(1);
        assertThat(jdbc.callsWith("UPDATE")).isEmpty();
    }

    @Test
    void userWithoutPasswordIsSkipped() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate().rows(WITHOUT_HASH, row("id", 6, "password", null));

        run(jdbc);

        assertThat(jdbc.callsWith("UPDATE")).isEmpty();
    }

    @Test
    void withoutTheV2SchemaTheServiceStillStarts() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate().fail(WITHOUT_HASH,
                new BadSqlGrammarException("backfill", "SELECT", new SQLException("column does not exist")));

        assertThatCode(() -> run(jdbc)).doesNotThrowAnyException();
    }

    private void run(RecordingJdbcTemplate jdbc) {
        new PasswordHashBackfill(new UserDao(jdbc), hasher).run(new DefaultApplicationArguments());
    }
}
