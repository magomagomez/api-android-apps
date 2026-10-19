package com.magomez.androidapps.jcfit.auth;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class JcfitAuthService {

    private static final Logger log = LoggerFactory.getLogger(JcfitAuthService.class);

    static final int TOKEN_BYTES = 32;

    private static final String INVALID_CREDENTIALS = "invalid_credentials";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Usuario o contraseña incorrectos";

    private final JcfitUserDao userDao;
    private final JcfitTokenDao tokenDao;
    private final JcfitPasswordHasher hasher;
    /** The accounts of the users table that may use each site; 0 (unset) lets nobody in there. */
    private final int appUserId;
    private final int adminUserId;
    private final SecureRandom random = new SecureRandom();
    /** Checked when the user does not exist, so a wrong name costs as much as a wrong password. */
    private final String decoyHash;

    public JcfitAuthService(JcfitUserDao userDao, JcfitTokenDao tokenDao, JcfitPasswordHasher hasher,
                            @Value("${jcfit.user-id:0}") int appUserId,
                            @Value("${jcfit.admin-user-id:0}") int adminUserId) {
        this.userDao = userDao;
        this.tokenDao = tokenDao;
        this.hasher = hasher;
        this.appUserId = appUserId;
        this.adminUserId = adminUserId;
        this.decoyHash = hasher.hash(newToken());
        if (appUserId <= 0) {
            log.warn("jcfit: JCFIT_USER_ID is not set, nobody can log in to the app");
        }
        if (adminUserId <= 0) {
            log.warn("jcfit: JCFIT_ADMIN_USER_ID is not set, nobody can log in to the backoffice");
        }
    }

    /**
     * Any failure is the same 401: no hint of whether the name or the password was wrong, of
     * whether the account exists for another app of the users table, or of whether it is the
     * account of the other site.
     */
    public JcfitLoginResponse login(JcfitLoginRequest request, JcfitRole role) {
        if (request == null || request.username() == null || request.password() == null) {
            throw invalidCredentials();
        }
        int allowedUserId = userIdOf(role);
        Optional<JcfitCredentials> user = userDao.findByName(request.username())
                .filter(found -> found.id() == allowedUserId && found.passwordHash() != null);
        String storedHash = user.map(JcfitCredentials::passwordHash).orElse(decoyHash);
        boolean matches = hasher.matches(request.password(), storedHash);
        if (user.isEmpty() || !matches) {
            throw invalidCredentials();
        }
        String token = newToken();
        tokenDao.insert(JcfitSha256.hex(token), user.get().id());
        return new JcfitLoginResponse(token);
    }

    /**
     * The user of a raw Bearer token, recording that the session was used. A session of an
     * account that no longer holds any role stops working; which site it may enter is
     * {@link #canEnter}'s call.
     */
    public Optional<JcfitCurrentUser> authenticate(String token) {
        return tokenDao.touch(JcfitSha256.hex(token))
                .filter(user -> canEnter(user, JcfitRole.APP) || canEnter(user, JcfitRole.ADMIN));
    }

    public boolean canEnter(JcfitCurrentUser user, JcfitRole role) {
        int allowedUserId = userIdOf(role);
        return allowedUserId > 0 && user.id() == allowedUserId;
    }

    public void logout(JcfitCurrentUser user) {
        tokenDao.delete(user.tokenHash());
    }

    private int userIdOf(JcfitRole role) {
        return role == JcfitRole.ADMIN ? adminUserId : appUserId;
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS, INVALID_CREDENTIALS_MESSAGE);
    }
}
