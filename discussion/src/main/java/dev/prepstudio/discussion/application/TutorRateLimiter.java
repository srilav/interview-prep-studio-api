package dev.prepstudio.discussion.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sliding-window limit on AI tutor requests per learner. In-memory: one instance per server. */
@Component
public class TutorRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int perMinute;
    private final Clock clock;
    private final Map<String, Deque<Instant>> calls = new ConcurrentHashMap<>();

    @Autowired
    public TutorRateLimiter(@Value("${prepstudio.discussion.tutor-requests-per-minute:5}") int perMinute) {
        this(perMinute, Clock.systemUTC());
    }

    TutorRateLimiter(int perMinute, Clock clock) {
        this.perMinute = perMinute;
        this.clock = clock;
    }

    public boolean tryAcquire(String learner) {
        Instant now = clock.instant();
        Deque<Instant> window = calls.computeIfAbsent(learner.toLowerCase(), k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(now.minus(WINDOW))) window.pollFirst();
            if (window.size() >= perMinute) return false;
            window.addLast(now);
            return true;
        }
    }
}
