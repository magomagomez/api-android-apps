package com.magomez.androidapps.restaurants.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * Writes to restaurant-api need the {@value #HEADER} header with the key from
 * {@code JCGOURMET_API_KEY}; reads stay public. Unlike travels-api there is no old app to keep
 * working, so the key is always enforced, and without a configured key every write is rejected.
 */
@Component
public class RestaurantApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final byte[] key;

    public RestaurantApiKeyFilter(@Value("${restaurants.api-key:}") String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ApiConfig.BASE_URL + "/")
                || READ_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (hasValidKey(request)) {
            chain.doFilter(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid " + HEADER);
    }

    private boolean hasValidKey(HttpServletRequest request) {
        String sent = request.getHeader(HEADER);
        return key.length > 0 && sent != null
                && MessageDigest.isEqual(key, sent.getBytes(StandardCharsets.UTF_8));
    }
}
