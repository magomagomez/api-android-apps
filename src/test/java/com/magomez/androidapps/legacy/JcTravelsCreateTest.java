package com.magomez.androidapps.legacy;

import com.magomez.androidapps.jctravels.cities.dao.CityDao;
import com.magomez.androidapps.jctravels.cities.dto.CreateCityRequest;
import com.magomez.androidapps.jctravels.cities.service.CityService;
import com.magomez.androidapps.jctravels.monuments.dao.MonumentDao;
import com.magomez.androidapps.jctravels.monuments.dto.CreateMonumentRequest;
import com.magomez.androidapps.jctravels.monuments.service.MonumentService;
import com.magomez.androidapps.jctravels.parks.dao.ParkDao;
import com.magomez.androidapps.jctravels.parks.dto.CreateParkRequest;
import com.magomez.androidapps.jctravels.parks.service.ParkScheduleService;
import com.magomez.androidapps.jctravels.parks.service.ParkService;
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
    private ParkService parks;
    private CityService cities;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        RouteDao routeDao = new RouteDao(jdbc);
        CityDao cityDao = new CityDao(jdbc);
        monuments = new MonumentService(new MonumentDao(jdbc), routeDao, cityDao);
        routes = new RouteService(routeDao);
        parks = new ParkService(new ParkDao(jdbc), cityDao, new ParkScheduleService());
        cities = new CityService(cityDao);
    }

    @Test
    void monumentGoesToTheRouteItIsGiven() {
        monuments.createMonument(new CreateMonumentRequest("Top of the Rock", 3, "8-24h", "NY Pass", 2));

        assertThat(jdbc.history).noneMatch(sql -> sql.contains("def"));
        assertThat(jdbc.history.get(0)).containsIgnoringCase("insert");
        assertThat(jdbc.argsHistory.get(0)).containsExactly("Top of the Rock", 3, "8-24h", "NY Pass", 2, 0);
    }

    @Test
    void monumentWithoutRouteStillGoesToTheCityDefault() {
        jdbc.nextResult = new Route(15, "Altres", 3, null, null, 0);

        monuments.createMonument(new CreateMonumentRequest("Toro", 3, null, null, null));

        assertThat(jdbc.argsHistory.get(1)).containsExactly("Toro", 3, null, null, 15, 0);
    }

    @Test
    void cityWithoutDefaultRouteNeedsARouteInsteadOfFailingWith500() {
        jdbc.nextFailure = new EmptyResultDataAccessException(1);

        assertThatThrownBy(() -> monuments.createMonument(new CreateMonumentRequest("Bellagio", 8, null, null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void aNewMonumentTurnsOnTheCityMonumentsFlag() {
        monuments.createMonument(new CreateMonumentRequest("Bellagio fountains", 8, null, null, 41));

        assertThat(jdbc.normalizedSql()).isEqualTo("UPDATE cities SET has_monuments = true WHERE id = ?");
        assertThat(jdbc.lastArgs).containsExactly(8);
    }

    @Test
    void aNewParkTurnsOnTheCityParksFlag() {
        parks.createPark(new CreateParkRequest("SeaWorld", 4));

        assertThat(jdbc.history.get(0)).containsIgnoringCase("insert");
        assertThat(jdbc.normalizedSql()).isEqualTo("UPDATE cities SET has_parks = true WHERE id = ?");
    }

    @Test
    void aNewCityCanSayWhatItHas() {
        cities.createCity(new CreateCityRequest("Miami", 1, false, true, true));

        assertThat(jdbc.normalizedSql()).contains("(name,travel,has_monuments,has_parks,has_outlets)");
        assertThat(jdbc.lastArgs).containsExactly("Miami", 1, false, true, true);
    }

    @Test
    void aNewCityWithoutFlagsHasNothingYet() {
        cities.createCity(new CreateCityRequest("Miami", 1, null, null, null));

        assertThat(jdbc.lastArgs).containsExactly("Miami", 1, false, false, false);
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
