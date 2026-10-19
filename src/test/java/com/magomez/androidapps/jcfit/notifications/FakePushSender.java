package com.magomez.androidapps.jcfit.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Hand-written {@link JcfitPushSender}: records every push and answers per device token. */
class FakePushSender implements JcfitPushSender {

    record Push(String deviceToken, String title, String body, Map<String, String> data) {
    }

    final List<Push> pushes = new ArrayList<>();
    private final Map<String, JcfitPushResult> results = new HashMap<>();
    private boolean configured = true;

    FakePushSender unconfigured() {
        configured = false;
        return this;
    }

    FakePushSender answer(String deviceToken, JcfitPushResult result) {
        results.put(deviceToken, result);
        return this;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public JcfitPushResult send(String deviceToken, String title, String body, Map<String, String> data) {
        pushes.add(new Push(deviceToken, title, body, Map.copyOf(data)));
        return results.getOrDefault(deviceToken, JcfitPushResult.DELIVERED);
    }
}
