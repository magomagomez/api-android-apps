package com.magomez.androidapps.jcfit.auth;

/**
 * The two sites of jcfit-api, each with its own account: the app (everything outside /admin)
 * and the backoffice (/admin). One account may hold both when both config vars name it.
 */
public enum JcfitRole {
    APP,
    ADMIN
}
