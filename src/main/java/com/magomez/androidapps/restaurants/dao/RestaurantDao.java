package com.magomez.androidapps.restaurants.dao;

import com.magomez.androidapps.restaurants.dto.Restaurant;
import com.magomez.androidapps.restaurants.dto.SaveRestaurantRequest;
import com.magomez.androidapps.restaurants.dto.VisitRequest;
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
    private static final String INSERT = """
            INSERT INTO gourmet_restaurants
                (name, city, cuisine, price_level, website, location, drive_folder_url, why_go)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
    private static final String UPDATE = """
            UPDATE gourmet_restaurants
            SET name = ?, city = ?, cuisine = ?, price_level = ?, website = ?, location = ?,
                drive_folder_url = ?, why_go = ?
            WHERE id = ?
            """;
    private static final String SAVE_VISIT = """
            UPDATE gourmet_restaurants
            SET visited_on = ?, rating = ?, comment = ?
            WHERE id = ?
            """;
    private static final String CLEAR_VISIT = """
            UPDATE gourmet_restaurants
            SET visited_on = NULL, rating = NULL, comment = NULL
            WHERE id = ?
            """;
    private static final String DELETE = "DELETE FROM gourmet_restaurants WHERE id = ?";

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

    /** Returns the id of the new restaurant. */
    public int insert(SaveRestaurantRequest restaurant) {
        Integer id = jdbcTemplate.queryForObject(INSERT, Integer.class, restaurant.name(), restaurant.city(),
                restaurant.cuisine(), restaurant.priceLevel(), restaurant.website(), restaurant.location(),
                restaurant.driveFolderUrl(), restaurant.whyGo());
        return id == null ? 0 : id;
    }

    /** The update methods return false when there is no restaurant with that id. */
    public boolean update(int id, SaveRestaurantRequest restaurant) {
        return jdbcTemplate.update(UPDATE, restaurant.name(), restaurant.city(), restaurant.cuisine(),
                restaurant.priceLevel(), restaurant.website(), restaurant.location(), restaurant.driveFolderUrl(),
                restaurant.whyGo(), id) > 0;
    }

    public boolean saveVisit(int id, VisitRequest visit) {
        return jdbcTemplate.update(SAVE_VISIT, visit.date(), visit.rating(), visit.comment(), id) > 0;
    }

    public boolean clearVisit(int id) {
        return jdbcTemplate.update(CLEAR_VISIT, id) > 0;
    }

    public boolean delete(int id) {
        return jdbcTemplate.update(DELETE, id) > 0;
    }
}
