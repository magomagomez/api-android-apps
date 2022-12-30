package com.magomez.androidapps.escapersthings.utils;

import java.util.function.Consumer;

public class ConverterUtils {
    private ConverterUtils(){}

    public static <T> void updateField(Consumer<T> target, T sourceField){
        if(sourceField != null){
            target.accept(sourceField);
        }
    }
}
