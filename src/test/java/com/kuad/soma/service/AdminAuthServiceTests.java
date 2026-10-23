package com.kuad.soma.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class AdminAuthServiceTests {
    @Test void productionRejectsMissingDefaultAndShortPasswords() {
        var env=new MockEnvironment(); env.setActiveProfiles("prod");
        for(String password:new String[]{""," ","change-me","short"})
            assertThatThrownBy(() -> new AdminAuthService(password,env)).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new AdminAuthService("unique-test-password-123",env)).doesNotThrowAnyException();
        env.setActiveProfiles("local","prod");
        assertThatThrownBy(() -> new AdminAuthService("change-me",env)).isInstanceOf(IllegalStateException.class);
    }
    @Test void expiryAndRevocation() {
        var env=new MockEnvironment();env.setActiveProfiles("local");
        class MutableClock extends Clock {
            Instant now=Instant.parse("2026-01-01T00:00:00Z");
            public ZoneId getZone(){return ZoneOffset.UTC;}
            public Clock withZone(ZoneId zone){return this;}
            public Instant instant(){return now;}
        }
        var clock=new MutableClock(); var auth=new AdminAuthService("change-me",env,clock);
        var first=auth.login("change-me").orElseThrow();
        var second=auth.login("change-me").orElseThrow();
        assertThat(first).hasSize(43).isNotEqualTo(second);
        assertThat(auth.isValid(first)).isTrue();
        auth.logout(first);assertThat(auth.isValid(first)).isFalse();
        clock.now=clock.now.plus(Duration.ofHours(8));
        assertThat(auth.isValid(second)).isFalse();
        assertThat(auth.login("wrong")).isEmpty();
        assertThat(auth.isValid(null)).isFalse();
    }
}
