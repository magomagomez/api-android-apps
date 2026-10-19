package com.magomez.androidapps.jcfit.notifications;

import com.magomez.androidapps.jcfit.admin.AdminText;
import com.magomez.androidapps.jcfit.config.ApiException;
import org.springframework.stereotype.Service;

/** The devices of the app account, so the backoffice notices reach them. */
@Service
public class JcfitDeviceService {

    static final int TOKEN_MAX = 4096;
    private static final String TOKEN_MESSAGE = "El token tiene que tener entre 1 y " + TOKEN_MAX + " caracteres";

    private final JcfitDeviceDao deviceDao;

    public JcfitDeviceService(JcfitDeviceDao deviceDao) {
        this.deviceDao = deviceDao;
    }

    public void register(int userId, JcfitDeviceRequest request) {
        deviceDao.upsert(token(request), userId);
    }

    /** Forgetting a token that was never registered is not an error. */
    public void unregister(JcfitDeviceRequest request) {
        deviceDao.delete(token(request));
    }

    private static String token(JcfitDeviceRequest request) {
        if (request == null) {
            throw ApiException.badRequest(TOKEN_MESSAGE);
        }
        return AdminText.required(request.token(), TOKEN_MAX, TOKEN_MESSAGE);
    }
}
