package com.magomez.androidapps.escapersthings.v2.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Component;

/**
 * On start-up, hashes the legacy plain-text password of every user still without
 * password_hash. Idempotent: users that already have a hash are never touched, so the
 * plain column (still used by v1) and later password changes stay independent.
 */
@Component
public class PasswordHashBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordHashBackfill.class);

    private final UserDao userDao;
    private final PasswordHasher hasher;

    public PasswordHashBackfill(UserDao userDao, PasswordHasher hasher) {
        this.userDao = userDao;
        this.hasher = hasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int filled = 0;
            for (UserDao.PlainPassword user : userDao.findWithoutPasswordHash()) {
                if (user.password() == null) {
                    log.warn("escapers v2: user {} has no password to hash", user.id());
                } else if (userDao.fillPasswordHash(user.id(), hasher.hash(user.password()))) {
                    filled++;
                }
            }
            if (filled > 0) {
                log.info("escapers v2: hashed the password of {} users", filled);
            }
        } catch (BadSqlGrammarException e) {
            // Without the v2 migration there is no password_hash column: v2 logins fail, but
            // the other apps of this service must still start.
            log.error("escapers v2: users.password_hash is missing, apply the v2 migration", e);
        }
    }
}
