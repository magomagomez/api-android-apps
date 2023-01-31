package com.magomez.androidapps.jcwedding.mail;

public enum UsersEnum {
    DEFAULT("appescapersthings@yahoo.com", "EscapersThings"),
    BARTOMEU("bartomeuamat@gmail.com", "Javi i Cris");

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
