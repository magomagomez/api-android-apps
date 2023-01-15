package com.magomez.androidapps.mustsee.movies.enums;

public enum FilmType {
    CINE(1),
    SERIE(2);

    private int id;

    private FilmType(int id){
        this.id = id;
    }

    public int getId(){ return this.id;}
    public static FilmType get(int type){
        for(FilmType filmType : values()){
            if(filmType.getId() == type){
                return filmType;
            }
        }
        return FilmType.CINE;
    }

}
