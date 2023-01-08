package com.magomez.androidapps.mustsee.mdb.converter;

import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsList;
import org.apache.commons.math3.util.Precision;

import java.util.List;

public class MovieDBConverter {

    private static final String URL = "https://image.tmdb.org/t/p/w600_and_h900_bestv2/";

    private MovieDBConverter(){}

    public static MovieDBDetailsDTO toDto(MovieDBDetails source) {
        MovieDBDetailsDTO dto = new MovieDBDetailsDTO();
        dto.setId(source.getId());
        dto.setAverage(Precision.round(source.getVoteAverage(),1));
        dto.setPosterUrl(URL + source.getPosterPath());
        if(source.getOriginalName() != null){
            dto.setOriginalTitle(source.getOriginalName());
        } else {
            dto.setOriginalTitle(source.getOriginalTitle());
        }
        if(source.getName() != null){
            dto.setTitle(source.getName());
        } else {
            dto.setTitle(source.getTitle());
        }
        if(source.getFirstAirDate() != null){
            dto.setReleaseDate(source.getFirstAirDate());
        } else {
            dto.setReleaseDate(source.getReleaseDate());
        }
        return dto;
    }

    public static List<MovieDBDetailsDTO> toDto(MovieDBDetailsList source) {
       return source.getResults().stream().map(MovieDBConverter::toDto).toList();
    }

}
