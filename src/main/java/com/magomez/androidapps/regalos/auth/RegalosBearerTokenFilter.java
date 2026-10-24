package com.magomez.androidapps.regalos.auth;

import com.magomez.androidapps.regalos.config.RegalosApi;
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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Every regalos-api route needs {@code Authorization: Bearer <token>}, reads included, except
 * POST /auth/register and POST /auth/login. The user of the token goes to the
 * {@link RegalosCurrentUser#ATTRIBUTE} request attribute.
 */
@Component
public class RegalosBearerTokenFilter extends OncePerRequestFilter {

    static final String REGISTER_PATH = RegalosApi.BASE_URL + "/auth/register";
    static final String LOGIN_PATH = RegalosApi.BASE_URL + "/auth/login";

    private static final Logger log = LoggerFactory.getLogger(RegalosBearerTokenFilter.class);
    private static final UrlPathHelper PATHS = UrlPathHelper.defaultInstance;
    private static final String PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_PATHS = Set.of(REGISTER_PATH, LOGIN_PATH);
    private static final Set<String> DOT_SEGMENTS = Set.of(".", "..");
    private static final String UNAUTHORIZED_BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Falta la sesión o ha caducado; vuelve a entrar\"}";
    private static final String SERVER_ERROR_BODY =
            "{\"error\":\"server_error\",\"message\":\"Error interno, inténtalo más tarde\"}";

    private final RegalosAuthService authService;

    public RegalosBearerTokenFilter(RegalosAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = path(request);
        boolean regalos = isRegalos(path) || (hasDotSegments(path) && isRegalos(StringUtils.cleanPath(path)));
        return !regalos || ("POST".equals(request.getMethod()) && PUBLIC_PATHS.contains(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (hasDotSegments(path(request))) {
            // Where "auth/login/../../people" ends up depends on who resolves the dots: never let it through.
            unauthorized(response);
            return;
        }
        Optional<RegalosCurrentUser> user;
        try {
            user = bearerToken(request).flatMap(authService::authenticate);
        } catch (DataAccessException e) {
            log.error("regalos-api: could not check the session", e);
            write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SERVER_ERROR_BODY);
            return;
        }
        if (user.isEmpty()) {
            unauthorized(response);
            return;
        }
        request.setAttribute(RegalosCurrentUser.ATTRIBUTE, user.get());
        chain.doFilter(request, response);
    }

    /**
     * Decoded, without ";params" and with "//" merged: the path Spring MVC routes by, so a
     * disguised URI cannot reach a regalos controller without passing through here.
     */
    private static String path(HttpServletRequest request) {
        return PATHS.getPathWithinApplication(request);
    }

    private static boolean isRegalos(String path) {
        return path.startsWith(RegalosApi.BASE_URL + "/");
    }

    private static boolean hasDotSegments(String path) {
        return Arrays.stream(path.split("/")).anyMatch(DOT_SEGMENTS::contains);
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return Optional.empty();
        }
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private static void unauthorized(HttpServletResponse response) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_BODY);
    }

    private static void write(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
