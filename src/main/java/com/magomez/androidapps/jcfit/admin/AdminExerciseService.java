package com.magomez.androidapps.jcfit.admin;

import com.magomez.androidapps.jcfit.config.ApiException;
import com.magomez.androidapps.jcfit.config.YouTubeUrls;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminExerciseService {

    static final int NAME_MAX = 80;
    static final int INSTRUCTIONS_MAX = 1000;
    static final int MUSCLE_GROUP_MAX = 60;

    static final String NOT_FOUND_MESSAGE = "El ejercicio no existe";
    private static final String NAME_MESSAGE =
            "El nombre es obligatorio y tiene como máximo " + NAME_MAX + " caracteres";
    private static final String YOUTUBE_MESSAGE = "El enlace de YouTube no es válido";
    private static final String INSTRUCTIONS_MESSAGE =
            "Las instrucciones tienen como máximo " + INSTRUCTIONS_MAX + " caracteres";
    private static final String MUSCLE_GROUP_MESSAGE =
            "El grupo muscular tiene como máximo " + MUSCLE_GROUP_MAX + " caracteres";

    private final AdminExerciseDao exerciseDao;

    public AdminExerciseService(AdminExerciseDao exerciseDao) {
        this.exerciseDao = exerciseDao;
    }

    public List<AdminExercise> list(boolean includeArchived) {
        return exerciseDao.findAll(includeArchived);
    }

    /** Always created active: archived is ignored here. */
    public AdminExercise create(AdminExerciseRequest request) {
        int id = exerciseDao.insert(clean(request));
        return get(id);
    }

    public AdminExercise update(int id, AdminExerciseRequest request) {
        if (!exerciseDao.update(id, clean(request))) {
            throw ApiException.notFound(NOT_FOUND_MESSAGE);
        }
        return get(id);
    }

    /** DELETE archives: past set logs keep pointing to the exercise. */
    public void archive(int id) {
        if (!exerciseDao.archive(id)) {
            throw ApiException.notFound(NOT_FOUND_MESSAGE);
        }
    }

    private AdminExercise get(int id) {
        return exerciseDao.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND_MESSAGE));
    }

    private static AdminExerciseRequest clean(AdminExerciseRequest request) {
        if (request == null) {
            throw ApiException.badRequest(NAME_MESSAGE);
        }
        String name = AdminText.required(request.name(), NAME_MAX, NAME_MESSAGE);
        String youtubeUrl = request.youtubeUrl() == null || request.youtubeUrl().isBlank()
                ? null : request.youtubeUrl().strip();
        if (youtubeUrl != null && YouTubeUrls.videoId(youtubeUrl).isEmpty()) {
            throw ApiException.badRequest(YOUTUBE_MESSAGE);
        }
        String instructions = AdminText.optional(request.instructions(), INSTRUCTIONS_MAX, INSTRUCTIONS_MESSAGE);
        String muscleGroup = AdminText.optional(request.muscleGroup(), MUSCLE_GROUP_MAX, MUSCLE_GROUP_MESSAGE);
        return new AdminExerciseRequest(name, youtubeUrl, instructions, muscleGroup, request.archived());
    }
}
