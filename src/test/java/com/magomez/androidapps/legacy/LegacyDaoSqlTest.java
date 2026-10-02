package com.magomez.androidapps.legacy;

import com.magomez.androidapps.escapersthings.escaperooms.dao.EscapeRoomsDao;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomFilter;
import com.magomez.androidapps.escapersthings.grades.dao.GradeDao;
import com.magomez.androidapps.friki.comics.dao.ComicDao;
import com.magomez.androidapps.mustsee.users.dao.UserFilmsDao;
import com.magomez.androidapps.mustsee.users.dto.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the SQL the legacy app DAOs (escapersthings, friki, jctravels, mustsee,
 * jcwedding) send: values must be bind parameters, and the queries fixed alongside
 * the parameterization must stay fixed.
 */
class LegacyDaoSqlTest {

    private static final String INJECTION = "x' OR '1'='1";

    private RecordingJdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
    }

    @Test
    void loginSendsCredentialsAsParametersNotSql() {
        LoginUser user = new LoginUser();
        user.setName(INJECTION);
        user.setPassword(INJECTION);

        new UserFilmsDao(jdbc).loginUser(user);

        assertThat(jdbc.lastSql).doesNotContain(INJECTION);
        assertThat(jdbc.lastArgs).containsExactly(INJECTION, INJECTION);
    }

    @Test
    void getUserNameIsAValidSelectById() {
        jdbc.nextResult = "Javi";

        String name = new UserFilmsDao(jdbc).getUserName(7);

        assertThat(name).isEqualTo("Javi");
        assertThat(jdbc.normalizedSql()).isEqualTo("select name from films_users where id = ?");
        assertThat(jdbc.lastArgs).containsExactly(7);
    }

    @Test
    void finishedFilterKeepsTheOrBranchesTogetherSoTypeStillApplies() {
        EscapeRoomsDao dao = new EscapeRoomsDao();
        ReflectionTestUtils.setField(dao, "jdbcTemplate", jdbc);

        dao.getAllEscapeRooms(new EscapeRoomFilter(1, null, INJECTION, null));

        assertThat(jdbc.normalizedSql()).contains(
                "WHERE 1=1 AND (done = 1 OR javi_done = 1 OR cris_done = 1) AND type = ?");
        assertThat(jdbc.lastSql).doesNotContain(INJECTION);
        assertThat(jdbc.lastArgs).containsExactly(INJECTION);
    }

    @Test
    void missingGradeIsNull() {
        GradeDao dao = gradeDao();
        jdbc.nextFailure = new EmptyResultDataAccessException(1);

        assertThat(dao.getGradeByUserId(3, 4)).isNull();
        assertThat(jdbc.lastArgs).containsExactly(3, 4);
    }

    @Test
    void gradeDatabaseFailureIsNotMistakenForMissingGrade() {
        GradeDao dao = gradeDao();
        jdbc.nextFailure = new DataAccessResourceFailureException("connection lost");

        assertThatThrownBy(() -> dao.getGradeByUserId(3, 4))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void missingComicIsNotFound() {
        jdbc.nextFailure = new EmptyResultDataAccessException(1);

        assertThatThrownBy(() -> new ComicDao(jdbc).getComic(9))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(jdbc.lastArgs).containsExactly(9);
    }

    @Test
    void comicDatabaseFailureIsNotReportedAsNotFound() {
        jdbc.nextFailure = new DataAccessResourceFailureException("connection lost");

        assertThatThrownBy(() -> new ComicDao(jdbc).getComic(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    private GradeDao gradeDao() {
        GradeDao dao = new GradeDao();
        ReflectionTestUtils.setField(dao, "jdbcTemplate", jdbc);
        return dao;
    }
}
