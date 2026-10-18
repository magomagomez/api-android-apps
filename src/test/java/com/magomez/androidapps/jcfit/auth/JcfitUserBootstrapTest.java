package com.magomez.androidapps.jcfit.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** The start-up job that creates the only user from config vars, when there is none yet. */
class JcfitUserBootstrapTest {

    private static final String ANY_USER = "SELECT EXISTS (SELECT 1 FROM jcfit_users)";
    private static final String INSERT = "INSERT INTO jcfit_users";
    private final JcfitPasswordHasher hasher = new JcfitPasswordHasher();

    @Test
    void emptyTableCreatesTheUserWithAHashedPassword() {
        JcfitRecordingJdbcTemplate jdbc = new JcfitRecordingJdbcTemplate().scalar(ANY_USER, false);

        run(jdbc, " magomez ", "secreta-larga");

        JcfitRecordingJdbcTemplate.Call insert = jdbc.callsWith(INSERT).get(0);
        assertThat(insert.args()[0]).isEqualTo("magomez");
        assertThat((String) insert.args()[1]).startsWith("pbkdf2$").doesNotContain("secreta-larga");
        assertThat(hasher.matches("secreta-larga", (String) insert.args()[1])).isTrue();
        assertThat(insert.sql()).contains("WHERE NOT EXISTS (SELECT 1 FROM jcfit_users)");
    }

    @Test
    void existingUserIsLeftAlone() {
        JcfitRecordingJdbcTemplate jdbc = new JcfitRecordingJdbcTemplate().scalar(ANY_USER, true);

        run(jdbc, "magomez", "secreta-larga");

        assertThat(jdbc.callsWith(INSERT)).isEmpty();
    }

    @Test
    void withoutPropertiesNothingIsDone() {
        JcfitRecordingJdbcTemplate jdbc = new JcfitRecordingJdbcTemplate().scalar(ANY_USER, false);

        run(jdbc, "", "");
        run(jdbc, "magomez", "  ");
        run(jdbc, null, "secreta-larga");

        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void databaseFailureDoesNotStopTheService() {
        JcfitRecordingJdbcTemplate missingTable = new JcfitRecordingJdbcTemplate().fail(ANY_USER,
                new BadSqlGrammarException("bootstrap", ANY_USER, new SQLException("relation does not exist")));
        JcfitRecordingJdbcTemplate down = new JcfitRecordingJdbcTemplate().scalar(ANY_USER, false)
                .fail(INSERT, new DataAccessResourceFailureException("connection refused"));

        assertThatCode(() -> run(missingTable, "magomez", "secreta-larga")).doesNotThrowAnyException();
        assertThatCode(() -> run(down, "magomez", "secreta-larga")).doesNotThrowAnyException();
    }

    private void run(JcfitRecordingJdbcTemplate jdbc, String username, String password) {
        new JcfitUserBootstrap(new JcfitUserDao(jdbc), hasher, username, password)
                .run(new DefaultApplicationArguments());
    }
}
