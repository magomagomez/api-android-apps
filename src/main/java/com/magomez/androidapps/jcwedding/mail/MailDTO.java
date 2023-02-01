package com.magomez.androidapps.jcwedding.mail;

import java.io.Serializable;

public class MailDTO implements Serializable {
    private String name;
    private String song;
    private String email;
    private Integer type;
    private String confirmMessage;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getConfirmMessage() {
        return confirmMessage;
    }

    public void setConfirmMessage(String confirmMessage) {
        this.confirmMessage = confirmMessage;
    }
}
