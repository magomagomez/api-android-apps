package com.magomez.androidapps.friki.funkos.converter;

import com.magomez.androidapps.friki.funkos.dto.Funko;
import com.magomez.androidapps.friki.funkos.dto.FunkoDTO;

import java.util.List;

public class FunkoConverter {

    private FunkoConverter(){}

    public static List<FunkoDTO> toDtoList(List<Funko> funkos){
        return funkos.stream()
                .map(FunkoConverter::toDto)
                .toList();
    }

    public static FunkoDTO toDto(Funko funko) {
        return new FunkoDTO(
                funko.id(),
                funko.name(),
                funko.isWishList(),
                funko.number(),
                funko.categories()
        );
    }

}
