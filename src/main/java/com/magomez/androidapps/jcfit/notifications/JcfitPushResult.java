package com.magomez.androidapps.jcfit.notifications;

/** What happened to one push: DEAD_TOKEN means the device is gone for good and can be forgotten. */
public enum JcfitPushResult {
    DELIVERED,
    DEAD_TOKEN,
    /** Credentials, network or the push service itself: nothing to do with that device. */
    FAILED
}
