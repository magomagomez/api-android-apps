package com.magomez.androidapps.mustsee.users.repository;

import com.magomez.androidapps.mustsee.users.dao.UserFilmsDao;
import com.magomez.androidapps.mustsee.users.dto.LoginUser;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.dto.UserLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRepository {

    private final UserFilmsDao usersDao;

    @Autowired
    public UserRepository(UserFilmsDao usersDao){
        this.usersDao = usersDao;
    }

    public String getUser(Integer userId){
        return usersDao.getUserName(userId);
    }

    public List<User> getRecommendationUserList(Integer userId){
        return usersDao.getRecommendationUserList(userId);
    }

    public List<User> getUserVisibility(Integer userId){
        return usersDao.getUserVisibility(userId);
    }

    public List<Integer> getUserWithFilmRecommended(Integer userId){
        return usersDao.getUserWithFilmRecommended(userId);
    }

    public UserLogin loginUser(LoginUser user) {
        return usersDao.loginUser(user);
    }

}
