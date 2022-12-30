package com.magomez.androidapps.escapersthings.users.controller;

import com.magomez.androidapps.escapersthings.config.ApiConfig;
import com.magomez.androidapps.escapersthings.users.dto.LoginUserRequest;
import com.magomez.androidapps.escapersthings.users.dto.UserLoginDTO;
import com.magomez.androidapps.escapersthings.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConfig.BASE_URL + "/users")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(final UserService userService){
        this.userService = userService;
    }

    @PostMapping
    public UserLoginDTO loginUser(@RequestBody LoginUserRequest loginUserRequest) {

        return userService.userLogin(loginUserRequest);
    }
}
