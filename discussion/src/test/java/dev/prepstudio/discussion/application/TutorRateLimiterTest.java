package dev.prepstudio.discussion.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TutorRateLimiterTest {

    @Test
    void allowsNPerMinutePerLearner() {
        var now = new AtomicReference<>(Instant.parse("2026-10-14T10:00:00Z"));
        Clock clock = new Clock() {
            public ZoneOffset getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId zone) { return this; }
            public Instant instant() { return now.get(); }
        };
        var limiter = new TutorRateLimiter(2, clock);

        assertThat(limiter.tryAcquire("Ada")).isTrue();
        assertThat(limiter.tryAcquire("ada")).isTrue();
        assertThat(limiter.tryAcquire("ada")).isFalse();
        assertThat(limiter.tryAcquire("grace")).isTrue();

        now.set(now.get().plus(Duration.ofSeconds(61)));
        assertThat(limiter.tryAcquire("ada")).isTrue();
    }
}
