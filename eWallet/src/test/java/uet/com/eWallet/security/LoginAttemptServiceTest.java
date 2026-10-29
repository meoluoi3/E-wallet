package uet.com.eWallet.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uet.com.eWallet.exception.AppException;
import uet.com.eWallet.exception.ErrorCode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        service = new LoginAttemptService(clock);
    }

    @Test
    void staysOpenBelowTheFailureLimit() {
        failTimes("alice", LoginAttemptService.MAX_FAILURES - 1);

        assertDoesNotThrow(() -> service.assertNotLocked("alice"));
    }

    @Test
    void locksAfterTooManyFailures() {
        failTimes("alice", LoginAttemptService.MAX_FAILURES);

        AppException exception = assertThrows(AppException.class, () -> service.assertNotLocked("alice"));
        assertEquals(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, exception.getErrorCode());
    }

    @Test
    void unlocksAfterLockTime() {
        failTimes("alice", LoginAttemptService.MAX_FAILURES);

        clock.advance(LoginAttemptService.LOCK_TIME);

        assertDoesNotThrow(() -> service.assertNotLocked("alice"));
    }

    @Test
    void successfulLoginClearsTheFailures() {
        failTimes("alice", LoginAttemptService.MAX_FAILURES - 1);
        service.reset("alice");
        failTimes("alice", LoginAttemptService.MAX_FAILURES - 1);

        assertDoesNotThrow(() -> service.assertNotLocked("alice"));
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        failTimes("alice", LoginAttemptService.MAX_FAILURES - 1);
        clock.advance(LoginAttemptService.WINDOW);
        failTimes("alice", LoginAttemptService.MAX_FAILURES - 1);

        assertDoesNotThrow(() -> service.assertNotLocked("alice"));
    }

    @Test
    void usernamesAreTrackedIndependently() {
        failTimes("alice", LoginAttemptService.MAX_FAILURES);

        assertDoesNotThrow(() -> service.assertNotLocked("bob"));
    }

    private void failTimes(String username, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(username);
        }
    }

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
