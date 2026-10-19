package com.magomez.androidapps.jcfit.notifications;

import java.util.Map;

/** Sends a push notification to one device; Firebase Cloud Messaging in production ({@link FcmPushSender}). */
public interface JcfitPushSender {

    /** False when the credentials are missing or unusable: nothing can be sent. */
    boolean isConfigured();

    /** Never throws for a delivery problem: the result says what went wrong. */
    JcfitPushResult send(String deviceToken, String title, String body, Map<String, String> data);
}
