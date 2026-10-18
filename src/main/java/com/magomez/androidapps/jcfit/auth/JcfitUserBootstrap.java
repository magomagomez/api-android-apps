package com.magomez.androidapps.jcfit.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * JCFit has no sign-up: on start-up, when jcfit_users is empty, creates the only user from
 * {@code jcfit.bootstrap.username} / {@code jcfit.bootstrap.password}. Never changes an
 * existing user, and never logs the password.
 */
@Component
public class JcfitUserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JcfitUserBootstrap.class);

    private final JcfitUserDao userDao;
    private final JcfitPasswordHasher hasher;
    private final String username;
    private final String password;

    public JcfitUserBootstrap(JcfitUserDao userDao, JcfitPasswordHasher hasher,
                              @Value("${jcfit.bootstrap.username:}") String username,
                              @Value("${jcfit.bootstrap.password:}") String password) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.username = username == null ? "" : username.trim();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isEmpty() || password == null || password.isBlank()) {
            log.info("jcfit: no bootstrap user configured (JCFIT_USERNAME / JCFIT_PASSWORD)");
            return;
        }
        try {
            if (userDao.anyUser()) {
                return;
            }
            if (userDao.insertFirst(username, hasher.hash(password))) {
                log.info("jcfit: created the user {}", username);
            }
        } catch (DataAccessException e) {
            // The same service runs other apps: they must start even if jcfit's tables are missing.
            log.error("jcfit: could not create the bootstrap user, is the jcfit migration applied?", e);
        }
    }
}
