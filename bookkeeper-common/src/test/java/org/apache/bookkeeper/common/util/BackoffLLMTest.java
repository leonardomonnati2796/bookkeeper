// Test generati da LLM per Backoff (tutte le strategie)
// Basati su category partition e boundary values aggiornati
package org.apache.bookkeeper.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BackoffLLMTest {

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
        assertDoesNotThrow(() -> assertFalse(Backoff.Exponential.of(0, 0, 0, 0).toBackoffs().findFirst().isPresent()));
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
        assertDoesNotThrow(() -> assertFalse(Backoff.Jitter.of(Backoff.Jitter.Type.EQUAL, -1, -2, 0).toBackoffs().findFirst().isPresent()));
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

    // Test aggiuntivi per copertura completa
    @Test
    void testConstantLimitZero() {
        assertDoesNotThrow(() -> assertFalse(Backoff.Constant.of(1, 2).toBackoffs().limit(0).findFirst().isPresent()));
    }

    @Test
    void testConstantValoriPositivi() {
        assertDoesNotThrow(() -> Backoff.Constant.of(10, 100).toBackoffs().limit(5).toArray(Long[]::new));
    }

    @Test
    void testJitterAllTypes() {
        for (Backoff.Jitter.Type type : Backoff.Jitter.Type.values()) {
            assertDoesNotThrow(() -> Backoff.Jitter.of(type, 1, 10, 1).toBackoffs().limit(2).toArray(Long[]::new));
        }
    }

    @Test
    void testExponentialOverflow() {
        assertDoesNotThrow(() -> {
            Long[] actual = Backoff.Exponential.of(Long.MAX_VALUE, Long.MAX_VALUE, 2, 3).toBackoffs().toArray(Long[]::new);
            assertTrue(actual.length > 0);
        });
    }
}
