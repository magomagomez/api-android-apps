package com.magomez.androidapps.mustsee;

import java.util.Arrays;

public enum Genre {
    Acción(28),
    Aventura(12),
    Animación(16),
    Comedia(35),
    Crimen(80),
    Documental(99),
    Drama(18),
    Familia(10751),
    Fantasia(14),
    Historia(36),
    Terror(27),
    Música(10402),
    Misterio(9648),
    Romance(10749),
    CienciaFiccion(878),
    TvFilm(10770),
    Suspense(53),
    Belica(10752),
    Western(37),
    Otros(0);

    private int id;

    private Genre(int id){
        this.id = id;
    }

    public int getId(){ return this.id;}
    public static Genre get(int type){
        for(Genre genre : values()){
            if(genre.getId() == type){
                return genre;
            }
        }
        return null;
    }

}
