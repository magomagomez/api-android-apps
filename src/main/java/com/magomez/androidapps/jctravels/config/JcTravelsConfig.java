package com.magomez.androidapps.jctravels.config;

public class JcTravelsConfig {

    public static final String SELECT = " select ";
    public static final String SELECT_ALL = SELECT + " * ";
    public static final String FROM = " from ";
    public static final String WHERE = " where ";
    public static final String INSERT = " insert into ";
    public static final String UPDATE = " update ";
    public static final String SET = " set ";
    public static final String VALUES = " values ";
    public static final String AND = " and ";

    private JcTravelsConfig(){
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }
}
