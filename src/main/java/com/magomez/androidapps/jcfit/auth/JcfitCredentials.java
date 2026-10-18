package com.magomez.androidapps.jcfit.auth;

/** A users row as the JCFit login needs it. */
record JcfitCredentials(int id, String name, String passwordHash) {
}
