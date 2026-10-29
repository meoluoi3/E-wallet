package uet.com.eWallet.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uet.com.eWallet.exception.AppException;
import uet.com.eWallet.exception.ErrorCode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits password guessing: after {@value #MAX_FAILURES} failed logins for the same username
 * within {@link #WINDOW}, that username is locked for {@link #LOCK_TIME}.
 * Unknown usernames are counted too, so the lock does not reveal which accounts exist.
 * State is kept in memory, so it applies per application instance.
 */
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK_TIME = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_USERNAMES = 10_000;

    private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {
        boolean isLockedAt(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        boolean isExpiredAt(Instant now) {
            return lockedUntil != null
                    ? !now.isBefore(lockedUntil)
                    : !now.isBefore(windowStart.plus(WINDOW));
        }
    }

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void assertNotLocked(String username) {
        Attempts current = attempts.get(username);
        if (current != null && current.isLockedAt(clock.instant())) {
            throw new AppException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }
    }

    public void recordFailure(String username) {
        Instant now = clock.instant();

        if (attempts.size() >= MAX_TRACKED_USERNAMES) {
            attempts.values().removeIf(entry -> entry.isExpiredAt(now));
        }

        attempts.compute(username, (key, current) -> {
            if (current == null || current.isExpiredAt(now)) {
                return new Attempts(1, now, null);
            }
            int failures = current.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_TIME) : null;
            return new Attempts(failures, current.windowStart(), lockedUntil);
        });
    }

    public void reset(String username) {
        attempts.remove(username);
    }
}
