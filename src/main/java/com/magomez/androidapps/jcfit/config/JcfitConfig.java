package com.magomez.androidapps.jcfit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class JcfitConfig {

    /** "Today" of JCFit is the day in Spain, not the server's UTC day. */
    public static final ZoneId SPAIN = ZoneId.of("Europe/Madrid");

    /** Inject with {@code @Qualifier("jcfitClock")}: other modules declare their own Clock. */
    @Bean
    public Clock jcfitClock() {
        return Clock.system(SPAIN);
    }
}
