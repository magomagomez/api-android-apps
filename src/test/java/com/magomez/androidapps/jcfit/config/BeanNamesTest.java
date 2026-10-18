package com.magomez.androidapps.jcfit.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every module of the app lives in one Spring context, so two components with the same
 * simple class name (two ApiExceptionHandler, say) stop the whole server from starting.
 * The unit tests never load that context; this test catches the clash instead.
 */
class BeanNamesTest {

    private static final String ROOT_PACKAGE = "com.magomez.androidapps";

    @Test
    void noTwoComponentsShareABeanName() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        AnnotationBeanNameGenerator names = AnnotationBeanNameGenerator.INSTANCE;
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        Map<String, List<String>> classesByName = scanner.findCandidateComponents(ROOT_PACKAGE).stream()
                .filter(definition -> !definition.getBeanClassName().contains("Test"))
                .collect(Collectors.groupingBy(
                        definition -> names.generateBeanName(definition, registry),
                        Collectors.mapping(BeanDefinition::getBeanClassName, Collectors.toList())));

        Map<String, List<String>> clashes = classesByName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(Map.of(), clashes);
    }
}
