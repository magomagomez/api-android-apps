package com.magomez.androidapps.mustsee.movies.service;

import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.service.MovieDBService;
import com.magomez.androidapps.mustsee.movies.converter.MovieConverter;
import com.magomez.androidapps.mustsee.movies.dto.Film;
import com.magomez.androidapps.mustsee.movies.dto.FilmDTO;
import com.magomez.androidapps.mustsee.movies.repository.MoviesRepository;
import com.magomez.androidapps.mustsee.users.dto.FilmRecomendation;
import com.magomez.androidapps.mustsee.users.dto.FilmRecomendationRequest;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MoviesService {

    private final MoviesRepository moviesRepository;
    private final UserRepository userRepository;
    private final MovieDBService movieDBService;

    @Autowired
    public MoviesService(MoviesRepository moviesRepository, UserRepository userRepository,
                         MovieDBService movieDBService){
        this.moviesRepository = moviesRepository;
        this.userRepository = userRepository;
        this.movieDBService = movieDBService;
    }

    public List<FilmDTO> getMovies(Integer userId){
        List<Film> movies = moviesRepository.getMovies(userId);
        List<User> users = userRepository.getRecommendationUserList(userId);
        List<FilmDTO> filmsDTO = MovieConverter.toDto(movies);
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


    public void insertRecomendation(Integer userId, Integer movieId, FilmRecomendationRequest film) throws Exception {
        MovieDBDetailsDTO movieDBDetailsDTO = movieDBService.getMovieDetails(movieId, film.getFilmType());
        FilmRecomendation filmRecomendation = MovieConverter.fromDto(userId,movieDBDetailsDTO,film);
        film.getFriendId().forEach(u->moviesRepository.insertRecomendation(filmRecomendation,u));
    }

    public void markFilmAsSeen(Integer userId, Integer movieId) {
        moviesRepository.markFilmAsSeen(userId,movieId);
    }

}
