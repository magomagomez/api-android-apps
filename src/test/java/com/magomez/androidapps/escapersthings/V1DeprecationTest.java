package com.magomez.androidapps.escapersthings;

import com.magomez.androidapps.escapersthings.config.EscaperStatusController;
import com.magomez.androidapps.escapersthings.config.V1DeprecationFilter;
import com.magomez.androidapps.escapersthings.escaperooms.controller.EscapeRoomsController;
import com.magomez.androidapps.escapersthings.grades.controller.GradeController;
import com.magomez.androidapps.escapersthings.mail.MailController;
import com.magomez.androidapps.escapersthings.users.controller.UserController;
import com.magomez.androidapps.escapersthings.userstats.controller.UserStatsController;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomsController;
import org.junit.jupiter.api.Test;
import org.springdoc.core.converters.SchemaPropertyDeprecatingConverter;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** v1 keeps working, but its responses and its OpenAPI say it is deprecated in favour of v2. */
class V1DeprecationTest {

    private static final List<Class<?>> V1_CONTROLLERS = List.of(EscapeRoomsController.class, GradeController.class,
            UserController.class, UserStatsController.class, MailController.class, EscaperStatusController.class);

    @Test
    void v1ResponsesCarryDeprecationAndSunset() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new V1DeprecationFilter().doFilter(new MockHttpServletRequest("GET", "/escapers-api/v1/escapeRooms"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getHeader("Deprecation")).isEqualTo("true");
        assertThat(response.getHeader("Sunset")).isEqualTo("Thu, 31 Dec 2026 23:59:59 GMT");
    }

    @Test
    void v2AndOtherApisAreNotMarked() throws Exception {
        for (String uri : List.of("/escapers-api/v2/escape-rooms", "/restaurant-api/v1/restaurants")) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            new V1DeprecationFilter().doFilter(new MockHttpServletRequest("GET", uri), response, chain);

            assertThat(chain.getRequest()).isNotNull();
            assertThat(response.getHeader("Deprecation")).isNull();
            assertThat(response.getHeader("Sunset")).isNull();
        }
    }

    @Test
    void everyV1OperationIsDeprecatedInOpenApi() {
        List<Method> operations = V1_CONTROLLERS.stream().flatMap(c -> handlerMethods(c).stream()).toList();

        assertThat(operations).hasSizeGreaterThan(V1_CONTROLLERS.size());
        assertThat(operations).allSatisfy(m -> assertThat(SchemaPropertyDeprecatingConverter.isDeprecated(m))
                .as(m.toString()).isTrue());
    }

    @Test
    void v2OperationsAreNotDeprecated() {
        assertThat(handlerMethods(RoomsController.class))
                .noneMatch(SchemaPropertyDeprecatingConverter::isDeprecated);
    }

    private static List<Method> handlerMethods(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(a -> a.annotationType().isAnnotationPresent(RequestMapping.class)))
                .toList();
    }
}
