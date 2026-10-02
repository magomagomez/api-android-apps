package com.magomez.androidapps.jctravels.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * Writes to travels-api (POST, PUT, DELETE...) need the {@value #HEADER} header with the key
 * from {@code JCTRAVELS_API_KEY}; reads stay public. With {@code JCTRAVELS_API_KEY_ENFORCE}
 * false a missing or wrong key is only logged, so the Cordova app keeps working until the
 * native app replaces it. Enforced without a configured key, every write is rejected.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final byte[] key;
    private final boolean enforce;

    public ApiKeyFilter(@Value("${jctravels.api-key:}") String key,
                        @Value("${jctravels.api-key-enforce:false}") boolean enforce) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
        this.enforce = enforce;
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
        if (!enforce) {
            LOG.warn("travels-api write without a valid {} header: {} {}", HEADER, request.getMethod(), request.getRequestURI());
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
