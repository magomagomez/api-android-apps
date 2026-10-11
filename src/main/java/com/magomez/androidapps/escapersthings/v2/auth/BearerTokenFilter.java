package com.magomez.androidapps.escapersthings.v2.auth;

import com.magomez.androidapps.escapersthings.v2.config.ApiConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Every escapers-api v2 route needs {@code Authorization: Bearer <token>}, reads included,
 * except the login itself. The user of the token goes to the {@link CurrentUser#ATTRIBUTE}
 * request attribute.
 */
@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = ApiConfig.BASE_URL + "/auth/login";

    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);
    private static final String PREFIX = "Bearer ";
    private static final String UNAUTHORIZED_BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Falta la sesión o ha caducado; vuelve a entrar\"}";
    private static final String SERVER_ERROR_BODY =
            "{\"error\":\"server_error\",\"message\":\"Error interno, inténtalo más tarde\"}";

    private final AuthService authService;

    public BearerTokenFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith(ApiConfig.BASE_URL + "/")
                || ("POST".equals(request.getMethod()) && LOGIN_PATH.equals(uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<String> token = bearerToken(request);
        Optional<CurrentUser> user;
        try {
            user = token.flatMap(authService::authenticate);
        } catch (DataAccessException e) {
            log.error("escapers-api v2: could not check the session", e);
            write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SERVER_ERROR_BODY);
            return;
        }
        if (user.isEmpty()) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            write(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_BODY);
            return;
        }
        request.setAttribute(CurrentUser.ATTRIBUTE, user.get());
        chain.doFilter(request, response);
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return Optional.empty();
        }
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private static void write(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
