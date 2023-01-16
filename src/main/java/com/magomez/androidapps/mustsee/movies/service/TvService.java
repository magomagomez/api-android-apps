package com.magomez.androidapps.mustsee.movies.service;

import com.magomez.androidapps.mustsee.movies.converter.MovieConverter;
import com.magomez.androidapps.mustsee.movies.dto.Film;
import com.magomez.androidapps.mustsee.movies.dto.FilmDTO;
import com.magomez.androidapps.mustsee.movies.repository.TvRepository;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TvService {

    private final TvRepository tvRepository;
    private final UserRepository userRepository;

    @Autowired
    public TvService(TvRepository tvRepository, UserRepository userRepository){
        this.tvRepository = tvRepository;
        this.userRepository = userRepository;
    }

    public List<FilmDTO> getTvShows(Integer userId){
        List<Film> tvShows = tvRepository.getTvShow(userId);
        List<User> users = userRepository.getRecommendationUserList(userId);
        List<FilmDTO> filmsDTO = MovieConverter.toDto(tvShows);
        fillUserName(users, filmsDTO);
        return filmsDTO;
    }

    private void fillUserName(List<User> users, List<FilmDTO> filmsDTO) {
        for (FilmDTO film : filmsDTO){
            for(User user : users){
                if(user.getId().equals(film.getFriend().getId())){
                    film.getFriend().setName(user.getName());
                }
            }
        }
    }

}
