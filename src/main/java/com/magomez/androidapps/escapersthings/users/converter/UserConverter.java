package com.magomez.androidapps.escapersthings.users.converter;

import com.magomez.androidapps.escapersthings.users.dto.LoginUserRequest;
import com.magomez.androidapps.escapersthings.users.dto.LoginUser;
import com.magomez.androidapps.escapersthings.users.dto.UserLogin;
import com.magomez.androidapps.escapersthings.users.dto.UserLoginDTO;


public class UserConverter {

    private UserConverter(){}

    public static LoginUser fromDto(LoginUserRequest user) {
        return new LoginUser(
                user.nombre(),
                user.password()
        );
    }

    public static UserLoginDTO toDto(UserLogin user) {
        return new UserLoginDTO(
                user.nombre(),
                user.id()
        );
    }

}
