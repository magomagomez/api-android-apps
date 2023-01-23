package com.magomez.androidapps.mustsee.users.service;

import com.magomez.androidapps.mustsee.users.converter.UserConverter;
import com.magomez.androidapps.mustsee.users.dto.LoginUserRequest;
import com.magomez.androidapps.mustsee.users.dto.LoginUser;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.dto.UserDTO;
import com.magomez.androidapps.mustsee.users.dto.UserLogin;
import com.magomez.androidapps.mustsee.users.dto.UserLoginDTO;
import com.magomez.androidapps.mustsee.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public class UserFilmsService {

    private final UserRepository userRepository;

    @Autowired
    public UserFilmsService(final UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public String getUserName(Integer userId){
        return userRepository.getUser(userId);
    }

    public List<UserDTO> getRecommendationUserList(Integer userId){
        List<User> userList = userRepository.getRecommendationUserList(userId);
        return UserConverter.toDtoList(userList.stream().filter(distinctByKey(User::getId)).toList());
    }

    public List<UserDTO> getUserVisibility(Integer userId, Integer filmId){
        List<User> userList = userRepository.getUserVisibility(userId);
        List<Integer> usersWhithFilms = userRepository.getUserWithFilmRecommended(filmId);
        return UserConverter.toDtoListWithFilms(userList,usersWhithFilms);
    }

    public UserLoginDTO userLogin(LoginUserRequest userDTO){
        LoginUser loginUser = UserConverter.fromDto(userDTO);
        UserLogin user = userRepository.loginUser(loginUser);
        return UserConverter.toDto(user);
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
