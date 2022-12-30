package com.magomez.androidapps.escapersthings.grades.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class Grade implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private BigDecimal enigma;
    private BigDecimal gameMaster;
    private BigDecimal inmersion;
    private BigDecimal horror;
    private BigDecimal global;
    private Integer userId;
    private Integer escapeRoomId;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getEscapeRoomId() {
        return escapeRoomId;
    }

    public void setEscapeRoomId(Integer escapeRoomId) {
        this.escapeRoomId = escapeRoomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getEnigma() {
        return enigma;
    }

    public void setEnigma(BigDecimal enigma) {
        this.enigma = enigma;
    }

    public BigDecimal getGameMaster() {
        return gameMaster;
    }

    public void setGameMaster(BigDecimal gameMaster) {
        this.gameMaster = gameMaster;
    }

    public BigDecimal getInmersion() {
        return inmersion;
    }

    public void setInmersion(BigDecimal inmersion) {
        this.inmersion = inmersion;
    }

    public BigDecimal getHorror() {
        return horror;
    }

    public void setHorror(BigDecimal horror) {
        this.horror = horror;
    }

    public BigDecimal getGlobal() {
        return global;
    }

    public void setGlobal(BigDecimal global) {
        this.global = global;
    }
}
