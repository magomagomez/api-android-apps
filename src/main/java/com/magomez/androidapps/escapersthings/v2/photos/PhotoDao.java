package com.magomez.androidapps.escapersthings.v2.photos;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PhotoDao {

    private static final String SELECT = """
            SELECT content_type, data, etag
            FROM escape_room_photos
            WHERE escape_room_id = ? AND kind = ?
            """;
    /** One photo per room and kind: uploading again replaces it. */
    private static final String UPSERT = """
            INSERT INTO escape_room_photos (escape_room_id, kind, content_type, data, etag, updated_at)
            VALUES (?, ?, ?, ?, ?, now())
            ON CONFLICT (escape_room_id, kind) DO UPDATE
            SET content_type = EXCLUDED.content_type, data = EXCLUDED.data, etag = EXCLUDED.etag,
                updated_at = EXCLUDED.updated_at
            """;

    private static final RowMapper<StoredPhoto> PHOTO = (rs, rowNum) ->
            new StoredPhoto(rs.getString("content_type"), rs.getBytes("data"), rs.getString("etag"));

    private final JdbcTemplate jdbcTemplate;

    public PhotoDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<StoredPhoto> find(int roomId, PhotoKind kind) {
        return jdbcTemplate.query(SELECT, PHOTO, roomId, kind.value()).stream().findFirst();
    }

    void save(int roomId, PhotoKind kind, StoredPhoto photo) {
        jdbcTemplate.update(UPSERT, roomId, kind.value(), photo.contentType(), photo.data(), photo.etag());
    }
}
