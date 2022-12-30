package com.magomez.androidapps.escapersthings.escaperooms.dto;

public enum UsersEnum {
    DEFAULT("appescapersthings@yahoo.com", "EscapersThings"),
    JAVI("magomagomez@gmail.com", "Magomez"),
    CRIS("crisdorca@gmail.com", "Cris"),
    URI("osilvest@xtec.cat", "Uri"),
    ALEX("carbonell.cos@gmail.com", "Alex"),
    SARA("willhazen@gmail.com", "Sara");

    private final String mail;
    private final String userName;

    UsersEnum(String mail, String name) {
        this.mail = mail;
        this.userName = name;
    }

    public UsersEnum getValue(String name) {
        for(UsersEnum e: UsersEnum.values()) {
            if(e.getUserName().equals(name)) {
                return e;
            }
        }
        return null;// not found
    }

    public String getMail() {
        return mail;
    }

    public String getUserName(){
        return userName;
    }


}
