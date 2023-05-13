package com.magomez.androidapps.mustsee.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.magomez.androidapps.mustsee.movies.dto.enums.FilmPlatformEnum;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilmRecomendation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String tittle;
    private String imageUrl;
    private FilmPlatformEnum platform;
    private List<Integer> userId;
    private Integer friendId;
    private Integer filmType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTittle() {
        return tittle;
    }

    public void setTittle(String tittle) {
        this.tittle = tittle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public FilmPlatformEnum getPlatform() {
        return platform;
    }

    public void setPlatform(FilmPlatformEnum platform) {
        this.platform = platform;
    }

    public List<Integer> getUserId() {
        return userId;
    }

    public void setUserId(List<Integer> userId) {
        this.userId = userId;
    }

    public Integer getFriendId() {
        return friendId;
    }

    public void setFriendId(Integer friendId) {
        this.friendId = friendId;
    }

    public Integer getFilmType() {
        return filmType;
    }

    public void setFilmType(Integer filmType) {
        this.filmType = filmType;
    }
}
