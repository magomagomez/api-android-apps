package com.magomez.androidapps;

import org.jetbrains.annotations.NotNull;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void configureContentNegotiation(@NotNull ContentNegotiationConfigurer configurer) {
                configurer.defaultContentType(MediaType.APPLICATION_JSON);
            }
        };
    }

    @Bean
    public GroupedOpenApi frikistanteriaGroup() {
        return GroupedOpenApi.builder()
                .group("frikistanteria") // nombre que aparecerá en Swagger UI
                .packagesToScan("com.magomez.androidapps.friki") // paquete a incluir
                .build();
    }

    @Bean
    public GroupedOpenApi escapersGroup() {
        return GroupedOpenApi.builder()
                .group("escapersthings") // nombre que aparecerá en Swagger UI
                .packagesToScan("com.magomez.androidapps.escapersthings") // paquete a incluir
                .build();
    }


}