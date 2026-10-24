package com.magomez.androidapps.regalos.auth;

import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.people.Birthday;
import com.magomez.androidapps.regalos.people.Me;
import com.magomez.androidapps.regalos.people.ProfileRules;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import com.magomez.androidapps.regalos.people.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class RegalosAuthService {

    private static final Logger log = LoggerFactory.getLogger(RegalosAuthService.class);

    static final int TOKEN_BYTES = 32;
    static final int MIN_PASSWORD = 8;
    static final int MAX_PASSWORD = 200;

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,40}");
    private static final String USER_KEY = "user:";
    private static final String IP_KEY = "ip:";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Usuario o contraseña incorrectos";

    private final RegalosUserDao userDao;
    private final RegalosTokenDao tokenDao;
    private final RegalosPasswordHasher hasher;
    private final LoginThrottle throttle;
    /** Empty (unset) closes the registration. */
    private final byte[] inviteCode;
    private final SecureRandom random = new SecureRandom();
    /** Checked when the user does not exist, so a wrong name costs as much as a wrong password. */
    private final String decoyHash;

    public RegalosAuthService(RegalosUserDao userDao, RegalosTokenDao tokenDao, RegalosPasswordHasher hasher,
                              LoginThrottle throttle, @Value("${regalos.invite-code:}") String inviteCode) {
        this.userDao = userDao;
        this.tokenDao = tokenDao;
        this.hasher = hasher;
        this.throttle = throttle;
        this.inviteCode = inviteCode == null ? new byte[0] : inviteCode.trim().getBytes(StandardCharsets.UTF_8);
        this.decoyHash = hasher.hash(newToken());
        if (this.inviteCode.length == 0) {
            log.warn("regalos: REGALOS_INVITE_CODE is not set, nobody can register");
        }
    }

    /**
     * The invite is checked before anything else, so without it nobody learns which usernames
     * exist. Wrong invites count against the IP.
     */
    public AuthResponse register(RegisterRequest request, String clientIp) {
        String ipKey = IP_KEY + clientIp;
        throttle.check(ipKey);
        if (request == null || !inviteMatches(request.inviteCode())) {
            throttle.failure(ipKey);
            throw new RegalosException(HttpStatus.FORBIDDEN, "invalid_invite", "El código de invitación no es válido");
        }
        String username = username(request.username());
        if (!USERNAME.matcher(username).matches()) {
            throw RegalosException.badRequest(
                    "El usuario debe tener entre 3 y 40 letras, números, puntos, guiones o guiones bajos");
        }
        String password = request.password();
        if (password == null || password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            throw RegalosException.badRequest("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres");
        }
        String displayName = ProfileRules.displayName(request.displayName());
        Birthday birthday = Birthday.valid(request.birthday());
        int id;
        try {
            id = userDao.insert(username, displayName, hasher.hash(password), birthday);
        } catch (DuplicateKeyException e) {
            throw new RegalosException(HttpStatus.CONFLICT, "username_taken", "Ese usuario ya existe; elige otro");
        }
        return new AuthResponse(openSession(id), new Me(id, username, displayName, birthday));
    }

    /** Any failure is the same 401: no hint of whether the name or the password was wrong. */
    public AuthResponse login(LoginRequest request, String clientIp) {
        String ipKey = IP_KEY + clientIp;
        String username = username(request == null ? null : request.username());
        String userKey = USER_KEY + username;
        throttle.check(ipKey, userKey);
        Optional<UserCredentials> user = request == null || request.password() == null
                ? Optional.empty() : userDao.findCredentials(username);
        String storedHash = user.map(UserCredentials::passwordHash).orElse(decoyHash);
        boolean matches = request != null && hasher.matches(request.password(), storedHash);
        Optional<Me> me = user.isPresent() && matches ? userDao.findMe(user.get().id()) : Optional.empty();
        if (me.isEmpty()) {
            throttle.failure(ipKey, userKey);
            throw new RegalosException(HttpStatus.UNAUTHORIZED, "invalid_credentials", INVALID_CREDENTIALS_MESSAGE);
        }
        throttle.success(userKey);
        return new AuthResponse(openSession(me.get().id()), me.get());
    }

    /** The user of a raw Bearer token, recording that the session was used. */
    public Optional<RegalosCurrentUser> authenticate(String token) {
        return tokenDao.touch(RegalosSha256.hex(token));
    }

    public void logout(RegalosCurrentUser user) {
        tokenDao.delete(user.tokenHash());
    }

    private boolean inviteMatches(String candidate) {
        if (inviteCode.length == 0 || candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(inviteCode, candidate.trim().getBytes(StandardCharsets.UTF_8));
    }

    private String openSession(int userId) {
        String token = newToken();
        tokenDao.insert(RegalosSha256.hex(token), userId);
        return token;
    }

    private static String username(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
