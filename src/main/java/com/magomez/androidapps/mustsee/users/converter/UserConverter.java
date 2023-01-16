package com.magomez.androidapps.mustsee.users.converter;

import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.dto.UserDTO;

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

}
