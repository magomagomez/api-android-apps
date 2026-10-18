package com.magomez.androidapps.jcfit.admin;

import com.magomez.androidapps.jcfit.auth.JcfitRecordingJdbcTemplate;
import com.magomez.androidapps.jcfit.config.ApiException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.assertApiError;
import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** The exercise catalogue of the backoffice: validation first, values always as parameters. */
class AdminExerciseServiceTest {

    static final String VIDEO = "dQw4w9WgXcQ";
    private static final String BY_ID = "FROM jcfit_exercises WHERE id = ?";

    private JcfitRecordingJdbcTemplate jdbc;
    private AdminExerciseService service;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        service = new AdminExerciseService(new AdminExerciseDao(jdbc));
    }

    static Map<String, Object> exercise(int id, String name, String youtubeUrl, boolean archived) {
        return row("id", id, "name", name, "youtube_url", youtubeUrl, "instructions", null,
                "muscle_group", "Femoral", "archived", archived);
    }

    @Test
    void listHidesTheArchivedByDefaultAndSortsByName() {
        jdbc.rows("FROM jcfit_exercises", exercise(4, "Peso muerto rumano", "https://youtu.be/" + VIDEO, false),
                exercise(2, "Remo", null, false));

        var exercises = service.list(false);

        assertThat(jdbc.last().sql()).contains("WHERE NOT archived").endsWith("ORDER BY name, id");
        assertThat(exercises).extracting(AdminExercise::name).containsExactly("Peso muerto rumano", "Remo");
        assertThat(exercises.get(0).youtubeVideoId()).isEqualTo(VIDEO);
        assertThat(exercises.get(1).youtubeVideoId()).isNull();
    }

    @Test
    void listCanIncludeTheArchived() {
        service.list(true);

        assertThat(jdbc.last().sql()).doesNotContain("WHERE").endsWith("ORDER BY name, id");
    }

    @Test
    void createTrimsStoresBlankAsNullAndReturnsTheExercise() {
        jdbc.scalar("INSERT INTO jcfit_exercises", 7).rows(BY_ID, exercise(7, "Press banca", null, false));

        AdminExercise created = service.create(new AdminExerciseRequest("  Press banca ", "  ", " ", " Pecho ", null));

        JcfitRecordingJdbcTemplate.Call insert = jdbc.calls.get(0);
        assertThat(insert.sql()).isEqualTo(
                "INSERT INTO jcfit_exercises (name, youtube_url, instructions, muscle_group) VALUES (?, ?, ?, ?) RETURNING id");
        assertThat(insert.args()).containsExactly("Press banca", null, null, "Pecho");
        assertThat(created.id()).isEqualTo(7);
        assertThat(created.archived()).isFalse();
    }

    @Test
    void nameIsRequiredAndUpToEightyCharacters() {
        jdbc.scalar("INSERT INTO jcfit_exercises", 7).rows(BY_ID, exercise(7, "x", null, false));

        assertInvalid(() -> service.create(new AdminExerciseRequest(null, null, null, null, null)));
        assertInvalid(() -> service.create(new AdminExerciseRequest("   ", null, null, null, null)));
        ApiException tooLong = assertInvalid(() -> service.create(new AdminExerciseRequest("a".repeat(81), null, null,
                null, null)));
        assertInvalid(() -> service.create(null));
        assertThat(tooLong.getMessage()).isEqualTo("El nombre es obligatorio y tiene como máximo 80 caracteres");
        assertThat(jdbc.calls).isEmpty();

        service.create(new AdminExerciseRequest("a".repeat(80), null, null, null, null));
        assertThat(jdbc.callsWith("INSERT")).hasSize(1);
    }

    @Test
    void youtubeLinkMustBeAVideo() {
        ApiException error = assertInvalid(() -> service.create(
                new AdminExerciseRequest("Remo", "https://vimeo.com/123", null, null, null)));

        assertThat(error.getMessage()).isEqualTo("El enlace de YouTube no es válido");
        assertInvalid(() -> service.create(new AdminExerciseRequest("Remo", "no es un enlace", null, null, null)));
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void validYoutubeLinkIsStoredTrimmed() {
        jdbc.scalar("INSERT INTO jcfit_exercises", 7).rows(BY_ID, exercise(7, "Remo", "https://youtu.be/" + VIDEO, false));

        AdminExercise created = service.create(new AdminExerciseRequest("Remo", " https://youtu.be/" + VIDEO + " ",
                null, null, null));

        assertThat(jdbc.calls.get(0).args()[1]).isEqualTo("https://youtu.be/" + VIDEO);
        assertThat(created.youtubeVideoId()).isEqualTo(VIDEO);
    }

    @Test
    void instructionsAndMuscleGroupHaveAMaximum() {
        assertThat(assertInvalid(() -> service.create(new AdminExerciseRequest("Remo", null, "a".repeat(1001), null,
                null))).getMessage()).isEqualTo("Las instrucciones tienen como máximo 1000 caracteres");
        assertThat(assertInvalid(() -> service.create(new AdminExerciseRequest("Remo", null, null, "a".repeat(61),
                null))).getMessage()).isEqualTo("El grupo muscular tiene como máximo 60 caracteres");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void updateCanArchiveAndKeepsTheFlagWhenItIsMissing() {
        jdbc.rows(BY_ID, exercise(4, "Remo", null, true));

        AdminExercise updated = service.update(4, new AdminExerciseRequest("Remo", null, null, null, true));
        service.update(4, new AdminExerciseRequest("Remo", null, null, null, null));

        JcfitRecordingJdbcTemplate.Call first = jdbc.callsWith("UPDATE jcfit_exercises").get(0);
        assertThat(first.sql()).isEqualTo("UPDATE jcfit_exercises SET name = ?, youtube_url = ?, instructions = ?, "
                + "muscle_group = ?, archived = COALESCE(?, archived) WHERE id = ?");
        assertThat(first.args()).containsExactly("Remo", null, null, null, true, 4);
        assertThat(jdbc.callsWith("UPDATE jcfit_exercises").get(1).args()[4]).isNull();
        assertThat(updated.archived()).isTrue();
    }

    @Test
    void updateValidatesAndUnknownIsNotFound() {
        assertInvalid(() -> service.update(4, new AdminExerciseRequest("", null, null, null, null)));
        assertThat(jdbc.calls).isEmpty();

        jdbc.updateCount("UPDATE jcfit_exercises", 0);
        ApiException error = assertApiError(() -> service.update(99, new AdminExerciseRequest("Remo", null, null, null,
                null)), HttpStatus.NOT_FOUND, "not_found");
        assertThat(error.getMessage()).isEqualTo("El ejercicio no existe");
    }

    @Test
    void deleteOnlyArchives() {
        service.archive(4);

        assertThat(jdbc.last().sql()).isEqualTo("UPDATE jcfit_exercises SET archived = true WHERE id = ?");
        assertThat(jdbc.last().args()).containsExactly(4);
        assertThat(jdbc.callsWith("DELETE")).isEmpty();
    }

    @Test
    void deleteOfAnUnknownExerciseIsNotFound() {
        jdbc.updateCount("SET archived = true", 0);

        assertApiError(() -> service.archive(99), HttpStatus.NOT_FOUND, "not_found");
    }

    private static ApiException assertInvalid(ThrowingCallable call) {
        return assertApiError(call, HttpStatus.BAD_REQUEST, "bad_request");
    }
}
