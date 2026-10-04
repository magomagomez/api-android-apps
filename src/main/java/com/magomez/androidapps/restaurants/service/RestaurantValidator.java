package com.magomez.androidapps.restaurants.service;

import com.magomez.androidapps.restaurants.dto.SaveRestaurantRequest;
import com.magomez.androidapps.restaurants.dto.VisitRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Checks the restaurant-api request bodies and returns them trimmed, with blank texts as null.
 * The limits match the columns of gourmet_restaurants.
 */
final class RestaurantValidator {

    private static final int MAX_NAME = 120;
    private static final int MAX_CITY = 80;
    private static final int MAX_CUISINE = 80;
    private static final int MAX_LINK = 500;
    private static final int MAX_NOTE = 2000;
    private static final int MIN_PRICE = 1;
    private static final int MAX_PRICE = 4;
    private static final BigDecimal MIN_RATING = BigDecimal.ONE;
    private static final BigDecimal MAX_RATING = BigDecimal.TEN;
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final Pattern WEB_ADDRESS = Pattern.compile("(?i)https?://\\S+");

    private RestaurantValidator() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static SaveRestaurantRequest restaurant(SaveRestaurantRequest request) {
        if (request == null) {
            throw badRequest("Missing body");
        }
        Integer price = request.priceLevel();
        if (price != null && (price < MIN_PRICE || price > MAX_PRICE)) {
            throw badRequest("price_level must be between 1 and 4");
        }
        return new SaveRestaurantRequest(
                required(request.name(), "name", MAX_NAME),
                required(request.city(), "city", MAX_CITY),
                optional(request.cuisine(), "cuisine", MAX_CUISINE),
                price,
                link(request.website(), "website"),
                optional(request.location(), "location", MAX_LINK),
                link(request.driveFolderUrl(), "drive_folder_url"),
                optional(request.whyGo(), "why_go", MAX_NOTE));
    }

    static VisitRequest visit(VisitRequest request, LocalDate today) {
        if (request == null || request.date() == null) {
            throw badRequest("date is required");
        }
        if (request.date().isAfter(today)) {
            throw badRequest("date cannot be in the future");
        }
        BigDecimal rating = request.rating();
        boolean halfPoints = rating != null && rating.multiply(TWO).stripTrailingZeros().scale() <= 0;
        if (!halfPoints || rating.compareTo(MIN_RATING) < 0 || rating.compareTo(MAX_RATING) > 0) {
            throw badRequest("rating must be between 1 and 10 in half points");
        }
        return new VisitRequest(request.date(), rating, optional(request.comment(), "comment", MAX_NOTE));
    }

    private static String required(String value, String field, int max) {
        String clean = optional(value, field, max);
        if (clean == null) {
            throw badRequest(field + " is required");
        }
        return clean;
    }

    private static String optional(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim();
        if (clean.length() > max) {
            throw badRequest(field + " is longer than " + max + " characters");
        }
        return clean;
    }

    private static String link(String value, String field) {
        String clean = optional(value, field, MAX_LINK);
        if (clean != null && !WEB_ADDRESS.matcher(clean).matches()) {
            throw badRequest(field + " must be an http(s) address");
        }
        return clean;
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
