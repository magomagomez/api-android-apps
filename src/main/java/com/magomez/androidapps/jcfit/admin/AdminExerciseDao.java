package com.magomez.androidapps.jcfit.admin;

import com.magomez.androidapps.jcfit.config.YouTubeUrls;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** The exercise catalogue. Exercises are never deleted (set logs point to them): they are archived. */
@Repository
public class AdminExerciseDao {

    private static final String SELECT =
            "SELECT id, name, youtube_url, instructions, muscle_group, archived FROM jcfit_exercises";
    private static final String NOT_ARCHIVED = " WHERE NOT archived";
    private static final String BY_NAME = " ORDER BY name, id";
    private static final String BY_ID = " WHERE id = ?";
    private static final String BY_IDS = " WHERE id IN (%s)";
    private static final String PLACEHOLDER = "?";
    private static final String INSERT = """
            INSERT INTO jcfit_exercises (name, youtube_url, instructions, muscle_group)
            VALUES (?, ?, ?, ?) RETURNING id
            """;
    private static final String UPDATE = """
            UPDATE jcfit_exercises SET name = ?, youtube_url = ?, instructions = ?, muscle_group = ?,
                archived = COALESCE(?, archived) WHERE id = ?
            """;
    private static final String ARCHIVE = "UPDATE jcfit_exercises SET archived = true WHERE id = ?";

    private static final RowMapper<AdminExercise> EXERCISE = (rs, rowNum) -> {
        String youtubeUrl = rs.getString("youtube_url");
        return new AdminExercise(rs.getInt("id"), rs.getString("name"), youtubeUrl,
                YouTubeUrls.videoId(youtubeUrl).orElse(null), rs.getString("instructions"),
                rs.getString("muscle_group"), rs.getBoolean("archived"));
    };

    private final JdbcTemplate jdbcTemplate;

    public AdminExerciseDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<AdminExercise> findAll(boolean includeArchived) {
        return jdbcTemplate.query(SELECT + (includeArchived ? "" : NOT_ARCHIVED) + BY_NAME, EXERCISE);
    }

    Optional<AdminExercise> findById(int id) {
        return jdbcTemplate.query(SELECT + BY_ID, EXERCISE, id).stream().findFirst();
    }

    /** Archived ones included; ids that do not exist are simply missing from the result. */
    List<AdminExercise> findByIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", Collections.nCopies(ids.size(), PLACEHOLDER));
        return jdbcTemplate.query(SELECT + BY_IDS.formatted(placeholders), EXERCISE, ids.toArray());
    }

    int insert(AdminExerciseRequest exercise) {
        Integer id = jdbcTemplate.queryForObject(INSERT, Integer.class, exercise.name(), exercise.youtubeUrl(),
                exercise.instructions(), exercise.muscleGroup());
        return id == null ? 0 : id;
    }

    /** False when no exercise has that id. */
    boolean update(int id, AdminExerciseRequest exercise) {
        return jdbcTemplate.update(UPDATE, exercise.name(), exercise.youtubeUrl(), exercise.instructions(),
                exercise.muscleGroup(), exercise.archived(), id) > 0;
    }

    /** False when no exercise has that id. */
    boolean archive(int id) {
        return jdbcTemplate.update(ARCHIVE, id) > 0;
    }
}
