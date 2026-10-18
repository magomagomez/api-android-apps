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
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Every jcfit-api route needs {@code Authorization: Bearer <token>}, reads included,
 * except the login itself. The user of the token goes to the {@link JcfitCurrentUser#ATTRIBUTE}
 * request attribute.
 */
@Component
public class JcfitBearerTokenFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = JcfitApi.BASE_URL + "/auth/login";

    private static final Logger log = LoggerFactory.getLogger(JcfitBearerTokenFilter.class);
    private static final UrlPathHelper PATHS = UrlPathHelper.defaultInstance;
    private static final String PREFIX = "Bearer ";
    private static final String UNAUTHORIZED_BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Falta la sesión o ha caducado; vuelve a entrar\"}";
    private static final String SERVER_ERROR_BODY =
            "{\"error\":\"server_error\",\"message\":\"Error interno, inténtalo más tarde\"}";

    private final JcfitAuthService authService;

    public JcfitBearerTokenFilter(JcfitAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Decoded, without ";params" and with "//" merged: the path Spring MVC routes by, so a
        // disguised URI cannot reach a jcfit controller without passing through here.
        String path = PATHS.getPathWithinApplication(request);
        return !path.startsWith(JcfitApi.BASE_URL + "/")
                || ("POST".equals(request.getMethod()) && LOGIN_PATH.equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
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
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            write(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_BODY);
            return;
        }
        request.setAttribute(JcfitCurrentUser.ATTRIBUTE, user.get());
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
