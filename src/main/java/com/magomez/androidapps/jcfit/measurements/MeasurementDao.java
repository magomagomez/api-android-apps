package com.magomez.androidapps.jcfit.measurements;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Reads and writes jcfit_measurements and reads the photo references of jcfit_photos. */
@Repository
public class MeasurementDao {

    private static final String SELECT_MEASUREMENTS = """
            SELECT id, measured_on, weight_kg, body_fat_pct, chest_cm, waist_cm, hip_cm, leg_cm, notes
            FROM jcfit_measurements
            """;
    private static final String NEWEST_FIRST = " ORDER BY measured_on DESC";
    private static final String BY_ID = " WHERE id = ?";
    private static final String SELECT_PHOTOS = "SELECT id, measurement_id, pose FROM jcfit_photos";
    private static final String PHOTOS_ORDER = " ORDER BY measurement_id, id";
    private static final String PHOTOS_OF_MEASUREMENT = " WHERE measurement_id = ? ORDER BY id";
    /** A second row on the same day breaks UNIQUE (measured_on): DuplicateKeyException. */
    private static final String INSERT = """
            INSERT INTO jcfit_measurements
                (measured_on, weight_kg, body_fat_pct, chest_cm, waist_cm, hip_cm, leg_cm, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """;
    private static final String UPDATE = """
            UPDATE jcfit_measurements SET measured_on = ?, weight_kg = ?, body_fat_pct = ?, chest_cm = ?,
                waist_cm = ?, hip_cm = ?, leg_cm = ?, notes = ?
            WHERE id = ?
            """;
    /** Its photo references go with it (ON DELETE CASCADE). */
    private static final String DELETE = "DELETE FROM jcfit_measurements WHERE id = ?";

    private static final RowMapper<MeasurementRow> MEASUREMENT = (rs, rowNum) -> new MeasurementRow(
            rs.getInt("id"), rs.getObject("measured_on", LocalDate.class),
            plain(rs.getBigDecimal("weight_kg")), plain(rs.getBigDecimal("body_fat_pct")),
            plain(rs.getBigDecimal("chest_cm")), plain(rs.getBigDecimal("waist_cm")),
            plain(rs.getBigDecimal("hip_cm")), plain(rs.getBigDecimal("leg_cm")), rs.getString("notes"));
    private static final RowMapper<MeasurementPhotoRow> PHOTO = (rs, rowNum) -> new MeasurementPhotoRow(
            rs.getInt("measurement_id"), new MeasurementPhoto(rs.getInt("id"), MeasurementPose.valueOf(rs.getString("pose"))));

    private final JdbcTemplate jdbcTemplate;

    public MeasurementDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<MeasurementRow> findAll() {
        return jdbcTemplate.query(SELECT_MEASUREMENTS + NEWEST_FIRST, MEASUREMENT);
    }

    Optional<MeasurementRow> findById(int id) {
        return jdbcTemplate.query(SELECT_MEASUREMENTS + BY_ID, MEASUREMENT, id).stream().findFirst();
    }

    List<MeasurementPhotoRow> findAllPhotos() {
        return jdbcTemplate.query(SELECT_PHOTOS + PHOTOS_ORDER, PHOTO);
    }

    List<MeasurementPhotoRow> findPhotos(int measurementId) {
        return jdbcTemplate.query(SELECT_PHOTOS + PHOTOS_OF_MEASUREMENT, PHOTO, measurementId);
    }

    /** Returns the id of the new row, taken from the column's sequence. */
    int insert(MeasurementRequest m) {
        Integer id = jdbcTemplate.queryForObject(INSERT, Integer.class, m.measuredOn(), m.weightKg(), m.bodyFatPct(),
                m.chestCm(), m.waistCm(), m.hipCm(), m.legCm(), m.notes());
        return id == null ? 0 : id;
    }

    /** update and delete return false when there is no measurement with that id. */
    boolean update(int id, MeasurementRequest m) {
        return jdbcTemplate.update(UPDATE, m.measuredOn(), m.weightKg(), m.bodyFatPct(), m.chestCm(), m.waistCm(),
                m.hipCm(), m.legCm(), m.notes(), id) > 0;
    }

    boolean delete(int id) {
        return jdbcTemplate.update(DELETE, id) > 0;
    }

    /** numeric(5,1) reads as 101.0: the app gets 101, and never 1.01E+2. */
    private static BigDecimal plain(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
