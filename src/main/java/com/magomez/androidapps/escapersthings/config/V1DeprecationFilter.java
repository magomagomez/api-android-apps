package com.magomez.androidapps.escapersthings.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * escapers-api v1 still works but is replaced by v2: every v1 response says so with the
 * Deprecation and Sunset headers (RFC 8594). Goes away with v1 at the cut-over.
 */
@Component
public class V1DeprecationFilter extends OncePerRequestFilter {

    public static final String DEPRECATION = "Deprecation";
    public static final String SUNSET = "Sunset";
    public static final String SUNSET_DATE = "Thu, 31 Dec 2026 23:59:59 GMT";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ApiConfig.BASE_URL + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Set before the chain runs, so they are there even if the response is committed early.
        response.setHeader(DEPRECATION, "true");
        response.setHeader(SUNSET, SUNSET_DATE);
        chain.doFilter(request, response);
    }
}
