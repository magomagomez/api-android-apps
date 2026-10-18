package com.magomez.androidapps.jcfit.admin;

import com.magomez.androidapps.jcfit.auth.JcfitRecordingJdbcTemplate;
import com.magomez.androidapps.jcfit.config.ApiException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.magomez.androidapps.jcfit.admin.AdminExerciseServiceTest.exercise;
import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.assertApiError;
import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** The weekly program of the backoffice: one active program, one workout per day of the week. */
class AdminProgramServiceTest {

    private static final String ACTIVE = "FROM jcfit_programs WHERE active";
    private static final String DAYS = "FROM jcfit_program_days WHERE program_id = ?";
    private static final String DAY_EXERCISES = "FROM jcfit_program_exercises pe";
    private static final String EXERCISES_BY_ID = "FROM jcfit_exercises WHERE id IN";
    private static final String UPSERT_DAY = "INSERT INTO jcfit_program_days";
    private static final LocalDate START = LocalDate.of(2026, 9, 1);

    private JcfitRecordingJdbcTemplate jdbc;
    private AdminProgramService service;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        service = new AdminProgramService(new AdminProgramDao(jdbc), new AdminExerciseDao(jdbc));
    }

    private void activeProgram() {
        jdbc.rows(ACTIVE, row("id", 1, "name", "Recomposición", "start_date", START));
    }

    private static AdminProgramExerciseRequest line(int exerciseId, int repsMin, int repsMax) {
        return new AdminProgramExerciseRequest(exerciseId, 4, repsMin, repsMax, 120);
    }

    private static AdminProgramDayRequest day(AdminProgramExerciseRequest... exercises) {
        return new AdminProgramDayRequest("Empuje", "Pecho · Hombro · Tríceps", List.of(exercises));
    }

    @Test
    void withoutActiveProgramIsNotFound() {
        ApiException error = assertApiError(() -> service.get(), HttpStatus.NOT_FOUND, "no_active_program");

        assertThat(error.getMessage()).isEqualTo("Todavía no hay un programa activo");
    }

    @Test
    void programHasItsDaysByDayOfWeekWithTheirExercisesInOrder() {
        activeProgram();
        jdbc.rows(DAYS, row("id", 3, "day_of_week", 1, "name", "Empuje", "muscles", "Pecho"),
                        row("id", 5, "day_of_week", 4, "name", "Pierna", "muscles", null))
                .rows(DAY_EXERCISES,
                        row("id", 11, "program_day_id", 3, "exercise_id", 4, "exercise_name", "Press banca",
                                "archived", false, "position", 1, "sets", 4, "reps_min", 6, "reps_max", 8,
                                "rest_seconds", 120),
                        row("id", 12, "program_day_id", 3, "exercise_id", 6, "exercise_name", "Fondos",
                                "archived", true, "position", 2, "sets", 3, "reps_min", 8, "reps_max", 12,
                                "rest_seconds", null));

        AdminProgram program = service.get();

        assertThat(program.id()).isEqualTo(1);
        assertThat(program.startDate()).isEqualTo(START);
        assertThat(program.days()).extracting(AdminProgramDay::dayOfWeek).containsExactly(1, 4);
        assertThat(program.days().get(0).exercises()).extracting(AdminProgramExercise::exerciseName)
                .containsExactly("Press banca", "Fondos");
        assertThat(program.days().get(0).exercises().get(1).archived()).isTrue();
        assertThat(program.days().get(0).exercises().get(1).restSeconds()).isNull();
        assertThat(program.days().get(1).exercises()).isEmpty();
        assertThat(jdbc.callsWith(DAYS).get(0).sql()).endsWith("ORDER BY day_of_week");
        assertThat(jdbc.callsWith(DAY_EXERCISES).get(0).args()).containsExactly(1);
    }

    @Test
    void saveCreatesTheActiveProgramWhenThereIsNone() {
        jdbc.scalar("INSERT INTO jcfit_programs", 1);

        AdminProgram created = service.save(new AdminProgramRequest(" Recomposición ", START));

        JcfitRecordingJdbcTemplate.Call insert = jdbc.callsWith("INSERT INTO jcfit_programs").get(0);
        assertThat(insert.sql()).isEqualTo(
                "INSERT INTO jcfit_programs (name, start_date, active) VALUES (?, ?, true) RETURNING id");
        assertThat(insert.args()).containsExactly("Recomposición", START);
        assertThat(jdbc.callsWith("UPDATE jcfit_programs")).isEmpty();
        assertThat(created).isEqualTo(new AdminProgram(1, "Recomposición", START, List.of()));
    }

    @Test
    void saveUpdatesTheActiveProgram() {
        activeProgram();

        AdminProgram program = service.save(new AdminProgramRequest("Volumen", START.plusDays(7)));

        JcfitRecordingJdbcTemplate.Call update = jdbc.callsWith("UPDATE jcfit_programs").get(0);
        assertThat(update.sql()).isEqualTo("UPDATE jcfit_programs SET name = ?, start_date = ? WHERE id = ?");
        assertThat(update.args()).containsExactly("Volumen", START.plusDays(7), 1);
        assertThat(jdbc.callsWith("INSERT")).isEmpty();
        assertThat(program.id()).isEqualTo(1);
    }

    @Test
    void saveNeedsANameAndAStartDate() {
        assertInvalid(() -> service.save(new AdminProgramRequest(" ", START)));
        assertInvalid(() -> service.save(new AdminProgramRequest("a".repeat(81), START)));
        assertInvalid(() -> service.save(new AdminProgramRequest("Volumen", null)));
        assertInvalid(() -> service.save(null));
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void replaceDayKeepsTheDayIdAndWritesTheExercisesInArrayOrder() {
        activeProgram();
        jdbc.scalar(UPSERT_DAY, 3)
                .rows(EXERCISES_BY_ID, exercise(4, "Press banca", null, false), exercise(9, "Aperturas", null, false));

        service.replaceDay(1, day(line(9, 10, 12), line(4, 6, 8), line(9, 12, 15)));

        JcfitRecordingJdbcTemplate.Call upsert = jdbc.callsWith(UPSERT_DAY).get(0);
        assertThat(upsert.sql()).contains("ON CONFLICT (program_id, day_of_week) DO UPDATE").endsWith("RETURNING id");
        assertThat(upsert.args()).containsExactly(1, 1, "Empuje", "Pecho · Hombro · Tríceps");
        assertThat(jdbc.callsWith(EXERCISES_BY_ID).get(0).sql()).contains("WHERE id IN (?, ?)");
        int deleteAt = indexOf("DELETE FROM jcfit_program_exercises WHERE program_day_id = ?");
        assertThat(jdbc.calls.get(deleteAt).args()).containsExactly(3);
        List<Object[]> inserted = new ArrayList<>();
        jdbc.callsWith("INSERT INTO jcfit_program_exercises").forEach(call -> inserted.add(call.args()));
        assertThat(inserted).containsExactly(
                new Object[]{3, 9, 1, 4, 10, 12, 120},
                new Object[]{3, 4, 2, 4, 6, 8, 120},
                new Object[]{3, 9, 3, 4, 12, 15, 120});
        assertThat(indexOf("INSERT INTO jcfit_program_exercises")).isGreaterThan(deleteAt);
    }

    @Test
    void replaceDayRunsInATransaction() throws Exception {
        assertThat(AdminProgramService.class.getMethod("replaceDay", int.class, AdminProgramDayRequest.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void archivedOrUnknownExercisesAreRejectedBeforeWriting() {
        activeProgram();
        jdbc.rows(EXERCISES_BY_ID, exercise(4, "Press banca", null, false), exercise(6, "Fondos", null, true));

        ApiException archived = assertInvalid(() -> service.replaceDay(1, day(line(4, 6, 8), line(6, 8, 12))));
        ApiException unknown = assertInvalid(() -> service.replaceDay(1, day(line(4, 6, 8), line(77, 8, 12))));

        assertThat(archived.getMessage()).isEqualTo("El ejercicio 6 está archivado");
        assertThat(unknown.getMessage()).isEqualTo("El ejercicio 77 no existe");
        assertNoWrites();
    }

    @Test
    void repsMaxBelowRepsMinIsRejected() {
        activeProgram();

        ApiException error = assertInvalid(() -> service.replaceDay(1, day(line(4, 10, 8))));

        assertThat(error.getMessage()).isEqualTo("Las repeticiones máximas van de las mínimas a 100");
        assertNoWrites();
    }

    @Test
    void everyFieldOfTheDayIsValidated() {
        activeProgram();

        assertInvalid(() -> service.replaceDay(0, day(line(4, 6, 8))));
        assertInvalid(() -> service.replaceDay(8, day(line(4, 6, 8))));
        assertInvalid(() -> service.replaceDay(1, null));
        assertInvalid(() -> service.replaceDay(1, new AdminProgramDayRequest(" ", null, List.of(line(4, 6, 8)))));
        assertInvalid(() -> service.replaceDay(1, new AdminProgramDayRequest("a".repeat(61), null, List.of(line(4, 6, 8)))));
        assertInvalid(() -> service.replaceDay(1, new AdminProgramDayRequest("Empuje", "a".repeat(121),
                List.of(line(4, 6, 8)))));
        assertThat(assertInvalid(() -> service.replaceDay(1, new AdminProgramDayRequest("Empuje", null, List.of())))
                .getMessage()).isEqualTo("Añade al menos un ejercicio");
        assertInvalid(() -> service.replaceDay(1, new AdminProgramDayRequest("Empuje", null, null)));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(null, 4, 6, 8, 120))));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(4, 0, 6, 8, 120))));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(4, 21, 6, 8, 120))));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(4, null, 6, 8, 120))));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(4, 4, 0, 8, 120))));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(4, 4, 6, 101, 120))));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(4, 4, 6, null, 120))));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(4, 4, 6, 8, -1))));
        assertInvalid(() -> service.replaceDay(1, day(new AdminProgramExerciseRequest(4, 4, 6, 8, 901))));
        assertNoWrites();
    }

    @Test
    void restIsOptionalAndLimitsAreInclusive() {
        activeProgram();
        jdbc.scalar(UPSERT_DAY, 3).rows(EXERCISES_BY_ID, exercise(4, "Press banca", null, false));

        service.replaceDay(7, new AdminProgramDayRequest("a".repeat(60), "a".repeat(120),
                List.of(new AdminProgramExerciseRequest(4, 20, 100, 100, null),
                        new AdminProgramExerciseRequest(4, 1, 1, 1, 900),
                        new AdminProgramExerciseRequest(4, 1, 1, 1, 0))));

        assertThat(jdbc.callsWith("INSERT INTO jcfit_program_exercises").get(0).args())
                .containsExactly(3, 4, 1, 20, 100, 100, null);
    }

    @Test
    void replaceDayWithoutActiveProgramIsNotFound() {
        assertApiError(() -> service.replaceDay(1, day(line(4, 6, 8))), HttpStatus.NOT_FOUND, "no_active_program");
        assertNoWrites();
    }

    @Test
    void deleteDayRemovesOnlyThatDayOfTheActiveProgram() {
        activeProgram();

        service.deleteDay(3);

        assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM jcfit_program_days WHERE program_id = ? AND day_of_week = ?");
        assertThat(jdbc.last().args()).containsExactly(1, 3);
    }

    @Test
    void deleteDayValidatesTheDayAndNeedsAProgram() {
        assertApiError(() -> service.deleteDay(3), HttpStatus.NOT_FOUND, "no_active_program");
        activeProgram();
        assertInvalid(() -> service.deleteDay(8));
        assertNoWrites();
    }

    private int indexOf(String sqlFragment) {
        for (int i = 0; i < jdbc.calls.size(); i++) {
            if (jdbc.calls.get(i).sql().contains(sqlFragment)) {
                return i;
            }
        }
        return -1;
    }

    private void assertNoWrites() {
        assertThat(jdbc.callsWith("INSERT")).isEmpty();
        assertThat(jdbc.callsWith("UPDATE")).isEmpty();
        assertThat(jdbc.callsWith("DELETE")).isEmpty();
    }

    private static ApiException assertInvalid(ThrowingCallable call) {
        return assertApiError(call, HttpStatus.BAD_REQUEST, "bad_request");
    }
}
