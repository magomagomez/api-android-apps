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
    /** The one account of the users table that may use JCFit; 0 (unset) lets nobody in. */
    private final int allowedUserId;
    private final SecureRandom random = new SecureRandom();
    /** Checked when the user does not exist, so a wrong name costs as much as a wrong password. */
    private final String decoyHash;

    public JcfitAuthService(JcfitUserDao userDao, JcfitTokenDao tokenDao, JcfitPasswordHasher hasher,
                            @Value("${jcfit.user-id:0}") int allowedUserId) {
        this.userDao = userDao;
        this.tokenDao = tokenDao;
        this.hasher = hasher;
        this.allowedUserId = allowedUserId;
        this.decoyHash = hasher.hash(newToken());
        if (allowedUserId <= 0) {
            log.warn("jcfit: JCFIT_USER_ID is not set, nobody can log in");
        }
    }

    /**
     * Any failure is the same 401: no hint of whether the name or the password was wrong, or
     * of whether the account exists for another app of the users table.
     */
    public JcfitLoginResponse login(JcfitLoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw invalidCredentials();
        }
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
     * account that is no longer the allowed one stops working.
     */
    public Optional<JcfitCurrentUser> authenticate(String token) {
        return tokenDao.touch(JcfitSha256.hex(token)).filter(user -> user.id() == allowedUserId);
    }

    public void logout(JcfitCurrentUser user) {
        tokenDao.delete(user.tokenHash());
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
