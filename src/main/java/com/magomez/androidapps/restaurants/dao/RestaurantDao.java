package com.magomez.androidapps.restaurants.dao;

import com.magomez.androidapps.restaurants.dto.Restaurant;
import com.magomez.androidapps.restaurants.mapper.RestaurantMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RestaurantDao {

    private static final String SELECT_RESTAURANTS = """
            SELECT id, name, city, cuisine, price_level, website, location, drive_folder_url, why_go,
                   visited_on, rating, comment
            FROM gourmet_restaurants
            """;
    private static final String PENDING = " WHERE visited_on IS NULL ORDER BY name";
    private static final String TRIED = " WHERE visited_on IS NOT NULL ORDER BY rating DESC, visited_on DESC, name";
    private static final String ALL = " ORDER BY name";
    private static final String BY_ID = " WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public RestaurantDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** visited null lists every restaurant; true only the tried ones, best rated first. */
    public List<Restaurant> findAll(Boolean visited) {
        String filter = visited == null ? ALL : visited ? TRIED : PENDING;
        return jdbcTemplate.query(SELECT_RESTAURANTS + filter, new RestaurantMapper());
    }

    public Optional<Restaurant> findById(int id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(SELECT_RESTAURANTS + BY_ID, new RestaurantMapper(), id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
