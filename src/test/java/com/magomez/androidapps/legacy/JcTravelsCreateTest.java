package com.magomez.androidapps.legacy;

import com.magomez.androidapps.jctravels.monuments.dao.MonumentDao;
import com.magomez.androidapps.jctravels.monuments.dto.CreateMonumentRequest;
import com.magomez.androidapps.jctravels.monuments.service.MonumentService;
import com.magomez.androidapps.jctravels.routes.dao.RouteDao;
import com.magomez.androidapps.jctravels.routes.dto.CreateRouteRequest;
import com.magomez.androidapps.jctravels.routes.dto.Route;
import com.magomez.androidapps.jctravels.routes.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Creating from the native app: a monument can go straight to a day route, a route can carry
 * its date. Without them everything behaves as for the Cordova app.
 */
class JcTravelsCreateTest {

    private RecordingJdbcTemplate jdbc;
    private MonumentService monuments;
    private RouteService routes;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        RouteDao routeDao = new RouteDao(jdbc);
        monuments = new MonumentService(new MonumentDao(jdbc), routeDao);
        routes = new RouteService(routeDao);
    }

    @Test
    void monumentGoesToTheRouteItIsGiven() {
        monuments.createMonument(new CreateMonumentRequest("Top of the Rock", 3, "8-24h", "NY Pass", 2));

        assertThat(jdbc.history).noneMatch(sql -> sql.contains("def"));
        assertThat(jdbc.lastArgs).containsExactly("Top of the Rock", 3, "8-24h", "NY Pass", 2, 0);
    }

    @Test
    void monumentWithoutRouteStillGoesToTheCityDefault() {
        jdbc.nextResult = new Route(15, "Altres", 3, null, null, 0);

        monuments.createMonument(new CreateMonumentRequest("Toro", 3, null, null, null));

        assertThat(jdbc.lastArgs).containsExactly("Toro", 3, null, null, 15, 0);
    }

    @Test
    void cityWithoutDefaultRouteNeedsARouteInsteadOfFailingWith500() {
        jdbc.nextFailure = new EmptyResultDataAccessException(1);

        assertThatThrownBy(() -> monuments.createMonument(new CreateMonumentRequest("Bellagio", 8, null, null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void routeKeepsItsDate() {
        routes.createRoute(new CreateRouteRequest("Divendres", 3, "19", "Agost"));

        assertThat(jdbc.normalizedSql()).contains("(name,city,def,day,month)");
        assertThat(jdbc.lastArgs).containsExactly("Divendres", 3, 0, "19", "Agost");
    }

    @Test
    void routeWithoutDateIsANamedGroup() {
        routes.createRoute(new CreateRouteRequest("Central Park", 3, null, null));

        assertThat(jdbc.lastArgs).containsExactly("Central Park", 3, 0, null, null);
    }
}
