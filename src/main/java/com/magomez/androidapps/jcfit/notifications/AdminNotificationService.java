package com.magomez.androidapps.jcfit.notifications;

import com.magomez.androidapps.jcfit.admin.AdminText;
import com.magomez.androidapps.jcfit.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Notices from the backoffice to every device of the app account, through {@link JcfitPushSender}. */
@Service
public class AdminNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationService.class);

    static final int TITLE_MAX = 60;
    static final int BODY_MAX = 300;
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;
    static final String MESSAGE_TYPE = "admin_message";

    private static final String TITLE_MESSAGE =
            "El título es obligatorio y tiene como máximo " + TITLE_MAX + " caracteres";
    private static final String BODY_MESSAGE = "El texto es obligatorio y tiene como máximo " + BODY_MAX + " caracteres";
    private static final String LIMIT_MESSAGE = "El límite tiene que ir de 1 a " + MAX_LIMIT;
    private static final String UNAVAILABLE = "notifications_unavailable";
    private static final String UNAVAILABLE_MESSAGE = "Las notificaciones todavía no están configuradas";
    private static final String FCM_ERROR = "fcm_error";
    private static final String FCM_ERROR_MESSAGE = "No se ha podido enviar el aviso; inténtalo más tarde";

    private final JcfitDeviceDao deviceDao;
    private final JcfitNotificationDao notificationDao;
    private final JcfitPushSender pushSender;
    /** The notices go to the devices of the app account (Cris). */
    private final int appUserId;

    public AdminNotificationService(JcfitDeviceDao deviceDao, JcfitNotificationDao notificationDao,
                                    JcfitPushSender pushSender, @Value("${jcfit.user-id:0}") int appUserId) {
        this.deviceDao = deviceDao;
        this.notificationDao = notificationDao;
        this.pushSender = pushSender;
        this.appUserId = appUserId;
    }

    /**
     * Saved only when it could be sent: without Firebase it is 503 (with or without devices, so
     * the backoffice learns it is missing), and when every device fails for a general reason it is
     * 502. Dead tokens are deleted and count as failed; no devices at all is saved with 0 delivered.
     */
    public AdminNotification send(AdminNotificationRequest request) {
        if (request == null) {
            throw ApiException.badRequest(TITLE_MESSAGE);
        }
        String title = AdminText.required(request.title(), TITLE_MAX, TITLE_MESSAGE);
        String body = AdminText.required(request.body(), BODY_MAX, BODY_MESSAGE);
        if (!pushSender.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE, UNAVAILABLE_MESSAGE);
        }
        List<String> tokens = deviceDao.tokensOf(appUserId);
        long id = notificationDao.nextId();
        Map<String, String> data = Map.of("type", MESSAGE_TYPE, "notificationId", String.valueOf(id));

        int delivered = 0;
        int failed = 0;
        List<String> dead = new ArrayList<>();
        for (String token : tokens) {
            JcfitPushResult result = pushSender.send(token, title, body, data);
            switch (result) {
                case DELIVERED -> delivered++;
                case DEAD_TOKEN -> dead.add(token);
                case FAILED -> failed++;
            }
        }
        if (!tokens.isEmpty() && failed == tokens.size()) {
            log.warn("jcfit: notice {} reached none of {} devices, not saved", id, tokens.size());
            throw new ApiException(HttpStatus.BAD_GATEWAY, FCM_ERROR, FCM_ERROR_MESSAGE);
        }
        deviceDao.deleteAll(dead);
        log.info("jcfit: notice {} delivered to {} devices, {} failed, {} dead tokens removed",
                id, delivered, failed + dead.size(), dead.size());
        return notificationDao.insert(id, title, body, delivered, failed + dead.size());
    }

    /** limit null means {@value #DEFAULT_LIMIT}. */
    public List<AdminNotification> list(Integer limit) {
        int rows = limit == null ? DEFAULT_LIMIT : limit;
        if (rows < 1 || rows > MAX_LIMIT) {
            throw ApiException.badRequest(LIMIT_MESSAGE);
        }
        return notificationDao.findRecent(rows);
    }
}
