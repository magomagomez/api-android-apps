package com.magomez.androidapps.restaurants.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RestaurantConfig {

    private static final ZoneId SPAIN = ZoneId.of("Europe/Madrid");

    /** "Today" for visit dates is Spain's, not the dyno's UTC. */
    @Bean
    public Clock gourmetClock() {
        return Clock.system(SPAIN);
    }
}
