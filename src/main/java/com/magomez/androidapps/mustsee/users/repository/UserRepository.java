package com.magomez.androidapps.mustsee.users.repository;

import com.magomez.androidapps.mustsee.users.dao.UserFilmsDao;
import com.magomez.androidapps.mustsee.users.dto.LoginUser;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.dto.UserLogin;
import com.magomez.androidapps.mustsee.users.dto.UserRecommend;
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

    public List<UserRecommend> getUserWithFilmRecommended(Integer filmId){
        return usersDao.getUserWithFilmRecommended(filmId);
    }

    public UserLogin loginUser(LoginUser user) {
        return usersDao.loginUser(user);
    }

}
