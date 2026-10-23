package com.kuad.soma.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Single-instance, in-memory sessions. Restarting the server signs everyone out. */
@Service
public class AdminAuthService {
    private final String password;
    private final Clock clock;
    private final Map<String, Instant> tokens = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private static final Duration LIFETIME = Duration.ofHours(8);

    @org.springframework.beans.factory.annotation.Autowired
    public AdminAuthService(@Value("${admin.password:}") String password, Environment environment) {
        this(password, environment, Clock.systemUTC());
    }

    AdminAuthService(String password, Environment environment, Clock clock) {
        boolean local = Arrays.asList(environment.getActiveProfiles()).contains("local")
                && !Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (password == null || password.isBlank() || (!local && (password.equals("change-me") || password.length() < 16))) {
            throw new IllegalStateException("ADMIN_PASSWORD must be set to a unique password of at least 16 characters outside local profile");
        }
        this.password = password;
        this.clock = clock;
    }

    public synchronized Optional<String> login(String supplied) {
        purgeExpired();
        if (supplied == null || supplied.length() > 1024 || !MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }
        // A bound prevents unbounded memory use; keep existing sessions when full.
        if (tokens.size() >= 100) return Optional.empty();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, clock.instant().plus(LIFETIME));
        return Optional.of(token);
    }

    public synchronized boolean isValid(String token) {
        purgeExpired();
        return token != null && tokens.containsKey(token);
    }

    public synchronized void logout(String token) { tokens.remove(token); }

    private void purgeExpired() {
        Instant now = clock.instant();
        tokens.values().removeIf(expiry -> !expiry.isAfter(now));
    }
}
