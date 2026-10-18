package com.magomez.androidapps.jcfit.auth;

/** A jcfit_users row as login needs it. */
record JcfitCredentials(int id, String username, String passwordHash) {
}
