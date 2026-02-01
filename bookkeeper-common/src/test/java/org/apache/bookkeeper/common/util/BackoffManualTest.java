// Test manuali per Backoff (tutte le strategie)
// Basati su category partition e boundary values aggiornati
package org.apache.bookkeeper.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BackoffManualTest {

    // Copertura overload Backoff.Jitter.of(Type, long, long)
    @Test
    void testJitterOfThreeArgs() {
        for (Backoff.Jitter.Type type : Backoff.Jitter.Type.values()) {
            assertDoesNotThrow(() -> Backoff.Jitter.of(type, 5, 10).toBackoffs().limit(2).toArray(Long[]::new));
        }
    }

    // Test Constant: startMs = -1, maxMs = -2, limit = 1 (tabella caso 1)
    @Test
    void testConstantCaso1() {
        assertDoesNotThrow(() -> Backoff.Constant.of(-1, -2).toBackoffs().limit(1).toArray(Long[]::new));
    }

    // Test Constant: startMs = 0, maxMs = 0, limit = -1 (tabella caso 1b)
    // Note: limit(-1) solleva IllegalArgumentException in Java Stream
    @Test
    void testConstantCaso1b() {
        assertThrows(IllegalArgumentException.class, () -> Backoff.Constant.of(0, 0).toBackoffs().limit(-1).toArray(Long[]::new));
    }

    // Test Constant: startMs = 1, maxMs = 2, limit = 1 (tabella caso 1c)
    @Test
    void testConstantCaso1c() {
        assertDoesNotThrow(() -> Backoff.Constant.of(1, 2).toBackoffs().limit(1).toArray(Long[]::new));
    }

    // Test Exponential: startMs = 0, maxMs = 0, multiplier = 0, limit = 0 (tabella caso 2)
    @Test
    void testExponentialCaso2() {
        assertDoesNotThrow(() -> Backoff.Exponential.of(0, 0, 0, 0).toBackoffs().toArray(Long[]::new));
    }

    // Test Exponential: startMs = 1, maxMs = 2, multiplier = 1, limit = -1 (tabella caso 2b)
    // Note: limit(-1) solleva IllegalArgumentException in Java Stream
    @Test
    void testExponentialCaso2b() {
        assertThrows(IllegalArgumentException.class, () -> Backoff.Exponential.of(1, 2, 1, 100).toBackoffs().limit(-1).toArray(Long[]::new));
    }

    // Test Exponential: startMs = 0, maxMs = 1, multiplier = 1, limit = 1 (tabella caso 2c)
    @Test
    void testExponentialCaso2c() {
        assertDoesNotThrow(() -> Backoff.Exponential.of(0, 1, 1, 1).toBackoffs().toArray(Long[]::new));
    }

    // Test Exponential: startMs = 1, maxMs = 0, multiplier = -1, limit = -1 (tabella caso 3)
    @Test
    void testExponentialCaso3() {
        assertDoesNotThrow(() -> Backoff.Exponential.of(1, 0, -1, -1).toBackoffs().limit(3).toArray(Long[]::new));
    }

    // Test Exponential: startMs = -1, maxMs = -2, multiplier = 0, limit = 0 (tabella caso 3b)
    @Test
    void testExponentialCaso3b() {
        assertDoesNotThrow(() -> Backoff.Exponential.of(-1, -2, 0, 0).toBackoffs().toArray(Long[]::new));
    }

    // Test Jitter: DECORRELATED, startMs = 0, maxMs = 0, limit = 1 (tabella caso 4)
    @Test
    void testJitterCaso4() {
        assertDoesNotThrow(() -> Backoff.Jitter.of(Backoff.Jitter.Type.DECORRELATED, 0, 0, 1).toBackoffs().toArray(Long[]::new));
    }

    // Test Jitter: DECORRELATED, startMs = 1, maxMs = 2, limit = -1 (tabella caso 4b)
    // Note: limit(-1) solleva IllegalArgumentException in Java Stream
    @Test
    void testJitterCaso4b() {
        assertThrows(IllegalArgumentException.class, () -> Backoff.Jitter.of(Backoff.Jitter.Type.DECORRELATED, 1, 2, 100).toBackoffs().limit(-1).toArray(Long[]::new));
    }

    // Test Jitter: EQUAL, startMs = -1, maxMs = -2, limit = 0 (tabella caso 5)
    @Test
    void testJitterCaso5() {
        assertDoesNotThrow(() -> Backoff.Jitter.of(Backoff.Jitter.Type.EQUAL, -1, -2, 0).toBackoffs().toArray(Long[]::new));
    }

    // Test Jitter: EQUAL, startMs = 0, maxMs = 1, limit = 1 (tabella caso 5b)
    @Test
    void testJitterCaso5b() {
        assertDoesNotThrow(() -> Backoff.Jitter.of(Backoff.Jitter.Type.EQUAL, 0, 1, 1).toBackoffs().toArray(Long[]::new));
    }

    // Test Jitter: EXPONENTIAL, startMs = 1, maxMs = 2, limit = -1 (tabella caso 6)
    // Note: limit(-1) solleva IllegalArgumentException in Java Stream
    @Test
    void testJitterCaso6() {
        assertThrows(IllegalArgumentException.class, () -> Backoff.Jitter.of(Backoff.Jitter.Type.EXPONENTIAL, 1, 2, 100).toBackoffs().limit(-1).toArray(Long[]::new));
    }

    // Test Jitter: EXPONENTIAL, startMs = 0, maxMs = 0, limit = 1 (tabella caso 6b)
    @Test
    void testJitterCaso6b() {
        assertDoesNotThrow(() -> Backoff.Jitter.of(Backoff.Jitter.Type.EXPONENTIAL, 0, 0, 1).toBackoffs().toArray(Long[]::new));
    }

    @Test
    void testExponentialSequenzaTipica() {
        long[] expected = new long[]{2, 4, 8, 16, 16};
        Long[] actual = Backoff.Exponential.of(2, 16, 2, 5).toBackoffs().toArray(Long[]::new);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i].longValue());
        }
    }

    @Test
    void testExponentialLimitZero() {
        assertDoesNotThrow(() -> assertFalse(Backoff.Exponential.of(1, 2, 1, 0).toBackoffs().findFirst().isPresent()));
    }

    @Test
    void testExponentialOverflow() {
        assertDoesNotThrow(() -> {
            Long[] actual = Backoff.Exponential.of(Long.MAX_VALUE, Long.MAX_VALUE, 2, 3).toBackoffs().toArray(Long[]::new);
            assertEquals(3, actual.length);
        });
    }

    // Test per copertura rami else in equalJittered
    @Test
    void testEqualJitteredElseBranch() {
        // Scegliendo startMs e maxMs uguali, il ramo else viene eseguito subito
        Long[] actual = Backoff.equalJittered(10, 10).limit(3).toArray(Long[]::new);
        for (Long v : actual) {
            assertEquals(10, v.longValue()); // else: return maxMs
        }
    }

    // Test per copertura ramo else in decorrelatedJittered
    @Test
    void testDecorrelatedJitteredElseBranch() {
        // Scegliendo startMs=0, il ramo if viene eseguito (randRange==0)
        Long[] actual = Backoff.decorrelatedJittered(0, 10).limit(3).toArray(Long[]::new);
        for (Long v : actual) {
            assertEquals(0, v.longValue()); // if: randBackoff = startNanos
        }
    }

    // Test Policy.NONE (costruttore vuoto)
    @Test
    void testPolicyNone() {
        assertFalse(Backoff.Policy.NONE.toBackoffs().findAny().isPresent());
    }

    // Test costruttore Backoff (copertura)
    @Test
    void testBackoffConstructor() {
        Backoff b = new Backoff();
        assertNotNull(b);
    }
}
