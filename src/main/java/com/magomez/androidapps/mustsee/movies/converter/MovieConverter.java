package com.magomez.androidapps.mustsee.movies.converter;

import com.magomez.androidapps.mustsee.movies.dto.Film;
import com.magomez.androidapps.mustsee.movies.dto.FilmDTO;
import com.magomez.androidapps.mustsee.movies.dto.Friend;

import java.util.List;

public class MovieConverter {

    private MovieConverter(){}

    public static List<FilmDTO> toDto(List<Film> source) {
        return source.stream().map(MovieConverter::toDto).toList();
    }

    public static FilmDTO toDto(Film source) {
        FilmDTO dto = new FilmDTO();
        dto.setId(source.getId());
        dto.setTitle(source.getTitle());
        dto.setPosterUrl(source.getPosterUrl());
        dto.setIdExternal(source.getIdExternal());
        dto.setIdUser(source.getIdUser());
        Friend friend = new Friend();
        friend.setId(source.getIdFriend());
        dto.setFriend(friend);
        dto.setPlatform(source.getPlatform());
        return dto;
    }

}
