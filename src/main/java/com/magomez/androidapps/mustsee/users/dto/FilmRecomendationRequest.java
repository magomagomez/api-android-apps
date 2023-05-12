package com.magomez.androidapps.mustsee.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.magomez.androidapps.mustsee.movies.dto.enums.FilmPlatformEnum;

import java.io.Serial;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilmRecomendationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("platform")
    private FilmPlatformEnum platform;
    @JsonProperty("friend_id")
    private Integer friendId;
    @JsonProperty("film_type")
    private Integer filmType;

    public FilmPlatformEnum getPlatform() {
        return platform;
    }

    public void setPlatform(FilmPlatformEnum platform) {
        this.platform = platform;
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
