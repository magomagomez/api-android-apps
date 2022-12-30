package com.magomez.androidapps;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableAutoConfiguration
@EnableWebMvc
@EnableJpaRepositories
@EnableScheduling
@ComponentScan(basePackages = "com.magomez.androidapps")
public class AndroidAppsApplication {

    public static void main(String... args) {
        new SpringApplicationBuilder(AndroidAppsApplication.class)
                .run(args);
    }

}