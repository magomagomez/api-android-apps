package com.magomez.androidapps.escapersthings.v2.rooms;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes escape_rooms for v2 and reads what a summary needs: players, grades and
 * photo etags. v2 ignores the v1 columns (done, javi_done, cris_done, *_total, image_url).
 */
@Repository
public class RoomDao {

    private static final String SELECT_ROOMS = "SELECT id, name, type, ubication FROM escape_rooms";
    private static final String ROOMS_BY_NAME = " ORDER BY name, id";
    private static final String ROOM_BY_ID = " WHERE id = ?";
    private static final String SELECT_PLAYERS = "SELECT escape_room_id, user_id, played_on FROM escape_room_players";
    private static final String PLAYERS_ORDER = " ORDER BY escape_room_id, user_id";
    private static final String PLAYERS_OF_ROOM = " WHERE escape_room_id = ? ORDER BY user_id";
    private static final String SELECT_GRADES = """
            SELECT g.escape_room_id, g.user_id, u.name AS user_name,
                   g.enigma, g.inmersion, g.horror, g.game_master, g.global
            FROM user_categoria_nota g
            JOIN users u ON u.id = g.user_id
            """;
    private static final String GRADES_ORDER = " ORDER BY g.escape_room_id, g.user_id";
    private static final String GRADES_OF_ROOM = " WHERE g.escape_room_id = ? ORDER BY g.user_id";
    private static final String SELECT_PHOTOS = "SELECT escape_room_id, kind, etag FROM escape_room_photos";
    private static final String PHOTOS_OF_ROOM = " WHERE escape_room_id = ?";
    /** The id comes from the column's sequence, never from MAX(id) + 1. */
    private static final String INSERT = "INSERT INTO escape_rooms (name, type, ubication) VALUES (?, ?, ?) RETURNING id";
    private static final String UPDATE = "UPDATE escape_rooms SET name = ?, type = ?, ubication = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM escape_rooms WHERE id = ?";
    private static final String EXISTS = "SELECT COUNT(*) FROM escape_rooms WHERE id = ?";

    private static final RowMapper<RoomRows.Room> ROOM = (rs, rowNum) -> new RoomRows.Room(
            rs.getInt("id"), rs.getString("name"), rs.getString("type"), rs.getString("ubication"));
    private static final RowMapper<RoomRows.Player> PLAYER = (rs, rowNum) -> new RoomRows.Player(
            rs.getInt("escape_room_id"), rs.getInt("user_id"), rs.getObject("played_on", LocalDate.class));
    private static final RowMapper<RoomRows.UserGradeRow> GRADE = (rs, rowNum) -> new RoomRows.UserGradeRow(
            rs.getInt("escape_room_id"), rs.getInt("user_id"), rs.getString("user_name"),
            new Grade(rs.getBigDecimal("enigma"), rs.getBigDecimal("inmersion"), rs.getBigDecimal("horror"),
                    rs.getBigDecimal("game_master"), rs.getBigDecimal("global")));
    private static final RowMapper<RoomRows.Photo> PHOTO = (rs, rowNum) -> new RoomRows.Photo(
            rs.getInt("escape_room_id"), rs.getString("kind"), rs.getString("etag"));

    private final JdbcTemplate jdbcTemplate;

    public RoomDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<RoomRows.Room> findAll() {
        return jdbcTemplate.query(SELECT_ROOMS + ROOMS_BY_NAME, ROOM);
    }

    Optional<RoomRows.Room> findById(int id) {
        return jdbcTemplate.query(SELECT_ROOMS + ROOM_BY_ID, ROOM, id).stream().findFirst();
    }

    List<RoomRows.Player> findAllPlayers() {
        return jdbcTemplate.query(SELECT_PLAYERS + PLAYERS_ORDER, PLAYER);
    }

    List<RoomRows.Player> findPlayers(int roomId) {
        return jdbcTemplate.query(SELECT_PLAYERS + PLAYERS_OF_ROOM, PLAYER, roomId);
    }

    List<RoomRows.UserGradeRow> findAllGrades() {
        return jdbcTemplate.query(SELECT_GRADES + GRADES_ORDER, GRADE);
    }

    List<RoomRows.UserGradeRow> findGrades(int roomId) {
        return jdbcTemplate.query(SELECT_GRADES + GRADES_OF_ROOM, GRADE, roomId);
    }

    List<RoomRows.Photo> findAllPhotos() {
        return jdbcTemplate.query(SELECT_PHOTOS, PHOTO);
    }

    List<RoomRows.Photo> findPhotos(int roomId) {
        return jdbcTemplate.query(SELECT_PHOTOS + PHOTOS_OF_ROOM, PHOTO, roomId);
    }

    public boolean exists(int id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS, Integer.class, id);
        return count != null && count > 0;
    }

    /** Returns the id of the new room. */
    int insert(SaveRoomRequest room) {
        Integer id = jdbcTemplate.queryForObject(INSERT, Integer.class, room.name(), room.category(), room.city());
        return id == null ? 0 : id;
    }

    /** update and delete return false when there is no room with that id. */
    boolean update(int id, SaveRoomRequest room) {
        return jdbcTemplate.update(UPDATE, room.name(), room.category(), room.city(), id) > 0;
    }

    boolean delete(int id) {
        return jdbcTemplate.update(DELETE, id) > 0;
    }
}
