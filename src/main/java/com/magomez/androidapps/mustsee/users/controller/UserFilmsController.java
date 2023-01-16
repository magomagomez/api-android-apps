package com.magomez.androidapps.mustsee.users.controller;

import com.magomez.androidapps.mustsee.config.ApiConfig;
import com.magomez.androidapps.mustsee.users.dto.UserDTO;
import com.magomez.androidapps.mustsee.users.service.UserFilmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConfig.BASE_URL + "/users")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
public class UserFilmsController {

    private final UserFilmsService userService;

    @Autowired
    public UserFilmsController(final UserFilmsService userService){
        this.userService = userService;
    }

    @GetMapping(value="/{userId}")
    public String getUserName(@PathVariable Integer userId){
        return userService.getUserName(userId);
    }

    @GetMapping(value="/{userId}/friends")
    public List<UserDTO> getRecommendationsUserList(@PathVariable Integer userId){
        return userService.getRecommendationUserList(userId);
    }
}
