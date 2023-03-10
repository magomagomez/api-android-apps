package com.magomez.androidapps.mustsee.mdb.converter;

import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsList;
import org.apache.commons.math3.util.Precision;

import java.util.List;
import java.util.Objects;

public class MovieDBConverter {

    private static final String URL = "https://image.tmdb.org/t/p/w600_and_h900_bestv2";
    private static final int MIN_VOTES = 10;

    private MovieDBConverter(){}

    public static MovieDBDetailsDTO toDto(MovieDBDetails source) {
        if (validateWrongFilms(source)) {
            return null;
        }
        MovieDBDetailsDTO dto = new MovieDBDetailsDTO();
        dto.setId(source.getId());
        dto.setAverage(Precision.round(source.getVoteAverage(), 1));
        dto.setPosterUrl(URL + source.getPosterPath());
        dto.setOverview(source.getOverview());
        fillMovieOrTv(source, dto);
        return dto;
    }

    private static void fillMovieOrTv(MovieDBDetails source, MovieDBDetailsDTO dto) {
        if (source.getOriginalName() != null) {
            dto.setOriginalTitle(source.getOriginalName());
            dto.setFilmType(2);
        } else {
            dto.setOriginalTitle(source.getOriginalTitle());
            dto.setFilmType(1);
        }

        if (source.getName() != null) {
            dto.setTitle(source.getName());
        } else {
            dto.setTitle(source.getTitle());
        }

        if (source.getFirstAirDate() != null) {
            dto.setReleaseDate(source.getFirstAirDate());
        } else {
            dto.setReleaseDate(source.getReleaseDate());
        }
    }

    private static boolean validateWrongFilms(MovieDBDetails source) {
        return Boolean.TRUE.equals(source.getAdult()) || source.getPosterPath() == null || source.getOverview() == null ||
                source.getVoteCont() < MIN_VOTES;
    }

    public static List<MovieDBDetailsDTO> toDto(MovieDBDetailsList source) {
       return source.getResults().stream().map(MovieDBConverter::toDto).filter(Objects::nonNull).toList();
    }

}
