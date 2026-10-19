package com.magomez.androidapps.jcfit.notifications;

/** Body of PUT and DELETE /devices: the FCM token of one installation of the app. */
public record JcfitDeviceRequest(String token) {
}
