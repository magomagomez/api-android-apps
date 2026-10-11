package com.magomez.androidapps.escapersthings.v2.auth;

import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import com.magomez.androidapps.escapersthings.v2.config.Sha256;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    static final int TOKEN_BYTES = 32;
    static final int MIN_PASSWORD_LENGTH = 8;

    private static final String INVALID_CREDENTIALS = "invalid_credentials";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Usuario o contraseña incorrectos";
    private static final String WEAK_PASSWORD = "weak_password";
    private static final String WEAK_PASSWORD_MESSAGE =
            "La contraseña nueva debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres";
    private static final String WRONG_PASSWORD = "wrong_password";
    private static final String WRONG_PASSWORD_MESSAGE = "La contraseña actual no es correcta";

    private final UserDao userDao;
    private final TokenDao tokenDao;
    private final PasswordHasher hasher;
    private final SecureRandom random = new SecureRandom();
    /** Checked when the user does not exist, so a wrong name costs as much as a wrong password. */
    private final String decoyHash;

    public AuthService(UserDao userDao, TokenDao tokenDao, PasswordHasher hasher) {
        this.userDao = userDao;
        this.tokenDao = tokenDao;
        this.hasher = hasher;
        this.decoyHash = hasher.hash(newToken());
    }

    /** Any failure is the same 401: no hint of whether the name or the password was wrong. */
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw invalidCredentials();
        }
        Optional<Credentials> user = userDao.findByName(request.username());
        String storedHash = user.map(Credentials::passwordHash).orElse(decoyHash);
        boolean matches = hasher.matches(request.password(), storedHash);
        if (user.isEmpty() || user.get().passwordHash() == null || !matches) {
            throw invalidCredentials();
        }
        String token = newToken();
        tokenDao.insert(Sha256.hex(token), user.get().id());
        return new LoginResponse(token, user.get().toDto());
    }

    /** The user of a raw Bearer token, recording that the session was used. */
    public Optional<CurrentUser> authenticate(String token) {
        return tokenDao.touch(Sha256.hex(token));
    }

    public void logout(CurrentUser user) {
        tokenDao.delete(user.tokenHash());
    }

    /** Needs the current password; every other session of the user stops working. */
    @Transactional
    public void changePassword(CurrentUser user, ChangePasswordRequest request) {
        if (request == null || request.newPassword() == null || request.newPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, WEAK_PASSWORD, WEAK_PASSWORD_MESSAGE);
        }
        String storedHash = userDao.findById(user.id()).map(Credentials::passwordHash).orElse(null);
        if (!hasher.matches(request.currentPassword(), storedHash)) {
            throw new ApiException(HttpStatus.FORBIDDEN, WRONG_PASSWORD, WRONG_PASSWORD_MESSAGE);
        }
        userDao.updatePasswordHash(user.id(), hasher.hash(request.newPassword()));
        tokenDao.deleteOthers(user.id(), user.tokenHash());
    }

    public List<UserDTO> users() {
        return userDao.findAll();
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
