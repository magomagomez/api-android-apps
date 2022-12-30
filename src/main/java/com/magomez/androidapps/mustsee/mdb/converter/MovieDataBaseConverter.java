package com.magomez.androidapps.mustsee.mdb.converter;

import com.magomez.androidapps.mustsee.mdb.dto.MovieDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetailsList;
import org.apache.commons.math3.util.Precision;

import java.util.List;

public class MovieDataBaseConverter {

    private static final String URL = "https://image.tmdb.org/t/p/w600_and_h900_bestv2/";

    private MovieDataBaseConverter(){}

    public static MovieDetailsDTO toDto(MovieDetails source) {
        MovieDetailsDTO dto = new MovieDetailsDTO();
        dto.setId(source.getId());
        dto.setAverage(Precision.round(source.getVoteAverage(),1));
        dto.setPosterUrl(URL + source.getPosterPath());
        dto.setOriginalTitle(source.getOriginalTitle());
        dto.setTitle(source.getTitle());
        dto.setReleaseDate(source.getReleaseDate());
        return dto;
    }

    public static List<MovieDetailsDTO> toDto(MovieDetailsList source) {
       return source.getResults().stream().map(MovieDataBaseConverter::toDto).toList();
    }

}
