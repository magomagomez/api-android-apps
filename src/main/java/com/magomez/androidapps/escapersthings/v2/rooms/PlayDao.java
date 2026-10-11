package com.magomez.androidapps.escapersthings.v2.rooms;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** Who played a room (escape_room_players) and each player's grade (user_categoria_nota). */
@Repository
public class PlayDao {

    private static final String DELETE_PLAYERS = "DELETE FROM escape_room_players WHERE escape_room_id = ?";
    private static final String INSERT_PLAYER =
            "INSERT INTO escape_room_players (escape_room_id, user_id, played_on) VALUES (?, ?, ?)";
    private static final String IS_PLAYER =
            "SELECT COUNT(*) FROM escape_room_players WHERE escape_room_id = ? AND user_id = ?";
    private static final String UPSERT_GRADE = """
            INSERT INTO user_categoria_nota (user_id, escape_room_id, enigma, inmersion, horror, game_master, global)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, escape_room_id) DO UPDATE
            SET enigma = EXCLUDED.enigma, inmersion = EXCLUDED.inmersion, horror = EXCLUDED.horror,
                game_master = EXCLUDED.game_master, global = EXCLUDED.global
            """;
    private static final String DELETE_GRADE =
            "DELETE FROM user_categoria_nota WHERE user_id = ? AND escape_room_id = ?";

    private final JdbcTemplate jdbcTemplate;

    public PlayDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Replaces the players of the room; their grades are left alone. */
    void replacePlayers(int roomId, List<Integer> userIds, LocalDate playedOn) {
        jdbcTemplate.update(DELETE_PLAYERS, roomId);
        if (!userIds.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_PLAYER,
                    userIds.stream().map(userId -> new Object[]{roomId, userId, playedOn}).toList());
        }
    }

    boolean isPlayer(int roomId, int userId) {
        Integer count = jdbcTemplate.queryForObject(IS_PLAYER, Integer.class, roomId, userId);
        return count != null && count > 0;
    }

    void saveGrade(int roomId, int userId, Grade grade) {
        jdbcTemplate.update(UPSERT_GRADE, userId, roomId, grade.enigma(), grade.immersion(), grade.terror(),
                grade.gameMaster(), grade.global());
    }

    void deleteGrade(int roomId, int userId) {
        jdbcTemplate.update(DELETE_GRADE, userId, roomId);
    }
}
