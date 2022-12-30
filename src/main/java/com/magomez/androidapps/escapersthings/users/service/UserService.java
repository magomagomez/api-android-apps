package com.magomez.androidapps.escapersthings.users.service;

import com.magomez.androidapps.escapersthings.users.dto.LoginUserRequest;
import com.magomez.androidapps.escapersthings.users.converter.UserConverter;
import com.magomez.androidapps.escapersthings.users.dto.LoginUser;
import com.magomez.androidapps.escapersthings.users.dto.UserLogin;
import com.magomez.androidapps.escapersthings.users.dto.UserLoginDTO;
import com.magomez.androidapps.escapersthings.users.dao.UsersDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UsersDao usersDao;

    @Autowired
    public UserService(final UsersDao usersDao){
        this.usersDao = usersDao;
    }

    public UserLoginDTO userLogin(LoginUserRequest userDTO){
        LoginUser loginUser = UserConverter.fromDto(userDTO);
        UserLogin user = usersDao.loginUser(loginUser);
        return UserConverter.toDto(user);
    }

}
