package com.magomez.androidapps.escapersthings.escaperooms.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public class UpdateEscapeRoom implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String imageUrl;
    private Integer done;
    private String ubication;
    private String type;
    private BigDecimal totalNota;
    private BigDecimal totalTerror;
    private Integer magoDone;
    private Integer crisDone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getDone() {
        return done;
    }

    public void setDone(Integer done) {
        this.done = done;
    }

    public String getUbication() {
        return ubication;
    }

    public void setUbication(String ubication) {
        this.ubication = ubication;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getTotalNota() {
        return totalNota;
    }

    public void setTotalNota(BigDecimal totalNota) {
        this.totalNota = totalNota;
    }

    public BigDecimal getTotalTerror() {
        return totalTerror;
    }

    public void setTotalTerror(BigDecimal totalTerror) {
        this.totalTerror = totalTerror;
    }

    public Integer getMagoDone() {
        return magoDone;
    }

    public void setMagoDone(Integer magoDone) {
        this.magoDone = magoDone;
    }

    public Integer getCrisDone() {
        return crisDone;
    }

    public void setCrisDone(Integer crisDone) {
        this.crisDone = crisDone;
    }
}
