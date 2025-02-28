package com.magomez.androidapps.friki.comics.converter;

import com.magomez.androidapps.friki.comics.dto.Comic;
import com.magomez.androidapps.friki.comics.dto.ComicDTO;

import java.util.List;

public class ComicConverter {

    private ComicConverter(){}

    public static List<ComicDTO> toDtoList(List<Comic> comics){
        return comics.stream()
                .map(ComicConverter::toDto)
                .toList();
    }

    public static ComicDTO toDto(Comic comic) {
        return new ComicDTO(
                comic.id(),
                comic.name(),
                comic.isWishList(),
                comic.categories()
        );
    }

}
