package com.magomez.androidapps.jcfit.notifications;

/** Body of POST /admin/notifications. */
public record AdminNotificationRequest(String title, String body) {
}
