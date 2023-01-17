package com.magomez.androidapps.mustsee.users.converter;

import com.magomez.androidapps.mustsee.users.dto.LoginUser;
import com.magomez.androidapps.mustsee.users.dto.LoginUserRequest;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.dto.UserDTO;
import com.magomez.androidapps.mustsee.users.dto.UserLogin;
import com.magomez.androidapps.mustsee.users.dto.UserLoginDTO;

import java.util.List;

public class UserConverter {

    private UserConverter(){}

    public static List<UserDTO> toDto(List<User> source) {
        return source.stream().map(UserConverter::toDto).toList();
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
