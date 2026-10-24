package com.magomez.androidapps.regalos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RegalosConfig {

    /** "Today" of regalos-api (birthdays, login throttling) is the day in Spain. */
    public static final ZoneId SPAIN = ZoneId.of("Europe/Madrid");

    /** Inject with {@code @Qualifier("regalosClock")}: other modules declare their own Clock. */
    @Bean
    public Clock regalosClock() {
        return Clock.system(SPAIN);
    }
}
