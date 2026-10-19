package com.magomez.androidapps.jcfit.auth;

import com.magomez.androidapps.jcfit.config.JcfitApi;
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
 * Every jcfit-api route needs {@code Authorization: Bearer <token>}, reads included, except
 * both logins. /admin routes are for the backoffice account and the rest for the app account:
 * a live token of the other one gets 403. The user of the token goes to the
 * {@link JcfitCurrentUser#ATTRIBUTE} request attribute.
 */
@Component
public class JcfitBearerTokenFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = JcfitApi.BASE_URL + "/auth/login";
    static final String ADMIN_LOGIN_PATH = JcfitApi.ADMIN_URL + "/auth/login";

    private static final Logger log = LoggerFactory.getLogger(JcfitBearerTokenFilter.class);
    private static final UrlPathHelper PATHS = UrlPathHelper.defaultInstance;
    private static final String PREFIX = "Bearer ";
    private static final Set<String> LOGIN_PATHS = Set.of(LOGIN_PATH, ADMIN_LOGIN_PATH);
    private static final Set<String> DOT_SEGMENTS = Set.of(".", "..");
    private static final String UNAUTHORIZED_BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Falta la sesión o ha caducado; vuelve a entrar\"}";
    private static final String FORBIDDEN_BODY =
            "{\"error\":\"forbidden\",\"message\":\"Esta cuenta no tiene acceso aquí\"}";
    private static final String SERVER_ERROR_BODY =
            "{\"error\":\"server_error\",\"message\":\"Error interno, inténtalo más tarde\"}";

    private final JcfitAuthService authService;

    public JcfitBearerTokenFilter(JcfitAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = path(request);
        boolean jcfit = isJcfit(path) || (hasDotSegments(path) && isJcfit(StringUtils.cleanPath(path)));
        return !jcfit || ("POST".equals(request.getMethod()) && LOGIN_PATHS.contains(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = path(request);
        if (hasDotSegments(path)) {
            // Which site "admin/../weeks" belongs to depends on who resolves the dots: never let it through.
            unauthorized(response);
            return;
        }
        Optional<String> token = bearerToken(request);
        Optional<JcfitCurrentUser> user;
        try {
            user = token.flatMap(authService::authenticate);
        } catch (DataAccessException e) {
            log.error("jcfit-api: could not check the session", e);
            write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SERVER_ERROR_BODY);
            return;
        }
        if (user.isEmpty()) {
            unauthorized(response);
            return;
        }
        if (!authService.canEnter(user.get(), roleFor(path))) {
            write(response, HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_BODY);
            return;
        }
        request.setAttribute(JcfitCurrentUser.ATTRIBUTE, user.get());
        chain.doFilter(request, response);
    }

    /**
     * Decoded, without ";params" and with "//" merged: the path Spring MVC routes by, so a
     * disguised URI cannot reach a jcfit controller without passing through here.
     */
    private static String path(HttpServletRequest request) {
        return PATHS.getPathWithinApplication(request);
    }

    private static boolean isJcfit(String path) {
        return path.startsWith(JcfitApi.BASE_URL + "/");
    }

    private static JcfitRole roleFor(String path) {
        return path.equals(JcfitApi.ADMIN_URL) || path.startsWith(JcfitApi.ADMIN_URL + "/")
                ? JcfitRole.ADMIN : JcfitRole.APP;
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
