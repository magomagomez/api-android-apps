package com.magomez.androidapps.jctravels.rides.dto;

public enum ParksName {
    UNIVERSAL_STUDIOS   (1),
    ISLANDS_ADVENTURE(2),
    EPCOT  (3),
    ANIMAL_KINGDOM  (4),
    HOLLYWOOD_STUDIOS   (5),
    MAGIC_KINGDOM(6);

    private Integer id;

    ParksName(Integer id){
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public static ParksName get(Integer id){
        for (ParksName park : values()){
            if (park.getId().equals(id)){
                return park;
            }
        }
        return null;
    }

    @Override
    public String toString(){
        return String.valueOf(id);
    }
}
