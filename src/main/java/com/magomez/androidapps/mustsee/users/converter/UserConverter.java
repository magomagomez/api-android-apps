package com.magomez.androidapps.mustsee.users.converter;

import com.magomez.androidapps.mustsee.users.dto.LoginUser;
import com.magomez.androidapps.mustsee.users.dto.LoginUserRequest;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.dto.UserDTO;
import com.magomez.androidapps.mustsee.users.dto.UserLogin;
import com.magomez.androidapps.mustsee.users.dto.UserLoginDTO;
import com.magomez.androidapps.mustsee.users.dto.UserRecommend;

import java.util.List;
import java.util.Optional;

public class UserConverter {

    private UserConverter(){}

    public static List<UserDTO> toDtoList(List<User> source) {
        return source.stream().map(UserConverter::toDto).toList();
    }

    public static List<UserDTO> toDtoListWithFilms(List<User> source, List<UserRecommend> userWithFilm) {
        return source.stream().map(u -> toDto(u,userWithFilm)).toList();
    }

    public static UserDTO toDto(User source, List<UserRecommend> userWithFilm) {
        UserDTO dto = new UserDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        Optional<UserRecommend> result = userWithFilm.stream()
                .filter(u-> u.getId().equals(source.getId()) || u.getFriendId().equals(source.getId()))
                .findAny();
        if(result.isPresent()){
            dto.setListed(true);
        }
        return dto;
    }

    public static UserDTO toDto(User source) {
        UserDTO dto = new UserDTO();
        dto.setId(source.getId());
        dto.setName(source.getName());
        return dto;
    }

    public static LoginUser fromDto(LoginUserRequest source) {
        LoginUser dto = new LoginUser();
        dto.setName(source.getName());
        dto.setPassword(source.getPassword());
        return dto;
    }

    public static UserLoginDTO toDto(UserLogin source) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setName(source.getName());
        dto.setId(source.getId());
        return dto;
    }

}
