package com.magomez.androidapps.jcfit.admin;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminProgramService {

    static final int PROGRAM_NAME_MAX = 80;
    static final int DAY_NAME_MAX = 60;
    static final int MUSCLES_MAX = 120;
    static final int MONDAY = 1;
    static final int SUNDAY = 7;
    static final int SETS_MIN = 1;
    static final int SETS_MAX = 20;
    static final int REPS_MIN = 1;
    static final int REPS_MAX = 100;
    static final int REST_MIN = 0;
    static final int REST_MAX = 900;

    private static final String NO_ACTIVE_PROGRAM = "no_active_program";
    private static final String NO_ACTIVE_PROGRAM_MESSAGE = "Todavía no hay un programa activo";
    private static final String PROGRAM_NAME_MESSAGE =
            "El nombre del programa es obligatorio y tiene como máximo " + PROGRAM_NAME_MAX + " caracteres";
    private static final String START_DATE_MESSAGE = "Falta la fecha de inicio del programa";
    private static final String DAY_OF_WEEK_MESSAGE = "El día de la semana va de 1 (lunes) a 7 (domingo)";
    private static final String DAY_NAME_MESSAGE =
            "El nombre del entreno es obligatorio y tiene como máximo " + DAY_NAME_MAX + " caracteres";
    private static final String MUSCLES_MESSAGE = "Los músculos tienen como máximo " + MUSCLES_MAX + " caracteres";
    private static final String NO_EXERCISES_MESSAGE = "Añade al menos un ejercicio";
    private static final String EXERCISE_MISSING_MESSAGE = "Falta el ejercicio";
    private static final String SETS_MESSAGE = "Las series van de " + SETS_MIN + " a " + SETS_MAX;
    private static final String REPS_MIN_MESSAGE =
            "Las repeticiones mínimas van de " + REPS_MIN + " a " + REPS_MAX;
    private static final String REPS_MAX_MESSAGE = "Las repeticiones máximas van de las mínimas a " + REPS_MAX;
    private static final String REST_MESSAGE = "El descanso va de " + REST_MIN + " a " + REST_MAX + " segundos";
    private static final String UNKNOWN_EXERCISE_MESSAGE = "El ejercicio %d no existe";
    private static final String ARCHIVED_EXERCISE_MESSAGE = "El ejercicio %d está archivado";

    private final AdminProgramDao programDao;
    private final AdminExerciseDao exerciseDao;

    public AdminProgramService(AdminProgramDao programDao, AdminExerciseDao exerciseDao) {
        this.programDao = programDao;
        this.exerciseDao = exerciseDao;
    }

    public AdminProgram get() {
        AdminProgramRows.Program program = activeProgram();
        Map<Integer, List<AdminProgramExercise>> lines = programDao.findLines(program.id()).stream()
                .collect(Collectors.groupingBy(AdminProgramRows.Line::programDayId,
                        Collectors.mapping(AdminProgramRows.Line::exercise, Collectors.toList())));
        List<AdminProgramDay> days = programDao.findDays(program.id()).stream()
                .map(day -> new AdminProgramDay(day.id(), day.dayOfWeek(), day.name(), day.muscles(),
                        lines.getOrDefault(day.id(), List.of())))
                .toList();
        return new AdminProgram(program.id(), program.name(), program.startDate(), days);
    }

    /** Creates the active program the first time; afterwards edits it. */
    @Transactional
    public AdminProgram save(AdminProgramRequest request) {
        if (request == null) {
            throw ApiException.badRequest(PROGRAM_NAME_MESSAGE);
        }
        String name = AdminText.required(request.name(), PROGRAM_NAME_MAX, PROGRAM_NAME_MESSAGE);
        if (request.startDate() == null) {
            throw ApiException.badRequest(START_DATE_MESSAGE);
        }
        Optional<AdminProgramRows.Program> active = programDao.findActive();
        if (active.isEmpty()) {
            int id = programDao.insertActive(name, request.startDate());
            return new AdminProgram(id, name, request.startDate(), List.of());
        }
        programDao.update(active.get().id(), name, request.startDate());
        return get();
    }

    /** Replaces the whole workout of that day; the day keeps its id if it already existed. */
    @Transactional
    public AdminProgram replaceDay(int dayOfWeek, AdminProgramDayRequest request) {
        requireDayOfWeek(dayOfWeek);
        if (request == null) {
            throw ApiException.badRequest(DAY_NAME_MESSAGE);
        }
        String name = AdminText.required(request.name(), DAY_NAME_MAX, DAY_NAME_MESSAGE);
        String muscles = AdminText.optional(request.muscles(), MUSCLES_MAX, MUSCLES_MESSAGE);
        List<AdminProgramExerciseRequest> lines = request.exercises();
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest(NO_EXERCISES_MESSAGE);
        }
        lines.forEach(AdminProgramService::validate);
        AdminProgramRows.Program program = activeProgram();
        requireUsableExercises(lines);
        int dayId = programDao.upsertDay(program.id(), dayOfWeek, name, muscles);
        programDao.replaceLines(dayId, lines);
        return get();
    }

    /** The day becomes a rest day; its sessions keep day_name and history (ON DELETE SET NULL). */
    public void deleteDay(int dayOfWeek) {
        requireDayOfWeek(dayOfWeek);
        programDao.deleteDay(activeProgram().id(), dayOfWeek);
    }

    private AdminProgramRows.Program activeProgram() {
        return programDao.findActive().orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, NO_ACTIVE_PROGRAM, NO_ACTIVE_PROGRAM_MESSAGE));
    }

    private static void requireDayOfWeek(int dayOfWeek) {
        if (dayOfWeek < MONDAY || dayOfWeek > SUNDAY) {
            throw ApiException.badRequest(DAY_OF_WEEK_MESSAGE);
        }
    }

    private static void validate(AdminProgramExerciseRequest line) {
        if (line == null || line.exerciseId() == null) {
            throw ApiException.badRequest(EXERCISE_MISSING_MESSAGE);
        }
        if (!between(line.sets(), SETS_MIN, SETS_MAX)) {
            throw ApiException.badRequest(SETS_MESSAGE);
        }
        if (!between(line.repsMin(), REPS_MIN, REPS_MAX)) {
            throw ApiException.badRequest(REPS_MIN_MESSAGE);
        }
        if (!between(line.repsMax(), line.repsMin(), REPS_MAX)) {
            throw ApiException.badRequest(REPS_MAX_MESSAGE);
        }
        if (line.restSeconds() != null && !between(line.restSeconds(), REST_MIN, REST_MAX)) {
            throw ApiException.badRequest(REST_MESSAGE);
        }
    }

    private void requireUsableExercises(List<AdminProgramExerciseRequest> lines) {
        Set<Integer> ids = lines.stream().map(AdminProgramExerciseRequest::exerciseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Integer, AdminExercise> found = exerciseDao.findByIds(ids).stream()
                .collect(Collectors.toMap(AdminExercise::id, Function.identity()));
        for (Integer id : ids) {
            AdminExercise exercise = found.get(id);
            if (exercise == null) {
                throw ApiException.badRequest(UNKNOWN_EXERCISE_MESSAGE.formatted(id));
            }
            if (exercise.archived()) {
                throw ApiException.badRequest(ARCHIVED_EXERCISE_MESSAGE.formatted(id));
            }
        }
    }

    private static boolean between(Integer value, int min, int max) {
        return value != null && value >= min && value <= max;
    }
}
