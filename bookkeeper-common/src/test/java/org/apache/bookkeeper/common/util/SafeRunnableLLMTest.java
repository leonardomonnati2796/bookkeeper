package org.apache.bookkeeper.common.util;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SafeRunnableLLMTest {
    @Test
    void testRunnableNull() {
        // Caso 6: Runnable null → handler non invocato perché NPE catturata
        SafeRunnable safeRunnable = SafeRunnable.safeRun(null, null);
        assertDoesNotThrow(safeRunnable::run);
    }

    @Test
    void testHandlerNullWithException() {
        Runnable r = () -> { throw new RuntimeException("test"); };
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, null);
        // Non rilancia, ma logga: qui si verifica che non venga rilanciata
        assertDoesNotThrow(safeRunnable::run);
    }

    @Test
    void testHandlerGestisceEccezione() {
        AtomicBoolean handled = new AtomicBoolean(false);
        Runnable r = () -> { throw new RuntimeException("test"); };
        Consumer<Throwable> handler = ex -> handled.set(true);
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        try {
            safeRunnable.run();
        } catch (RuntimeException e) {
            // Ignora rilancio
        }
        assertTrue(handled.get());
    }

    @Test
    void testHandlerRilanciaEccezione() {
        // Caso 5: Handler che rilancia → eccezione catturata dal run() esterno
        AtomicBoolean handled = new AtomicBoolean(false);
        Runnable r = () -> { throw new RuntimeException("errore"); };
        Consumer<Throwable> handler = ex -> {
            handled.set(true);
            throw new RuntimeException("rilanciata");
        };
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        // L'eccezione viene catturata e loggata, NON propagata
        assertDoesNotThrow(safeRunnable::run);
        assertTrue(handled.get());
    }

    @Test
    void testRunnableTerminaNormalmente() {
        // Caso 1: Runnable normale senza handler - esecuzione corretta
        Runnable r = () -> {};
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, null);
        assertDoesNotThrow(safeRunnable::run);
    }

    @Test
    void testRunnableNormaleConHandlerNonInvocato() {
        // Caso 2: Runnable normale con handler valido - handler NON deve essere invocato
        AtomicBoolean handlerInvoked = new AtomicBoolean(false);
        Runnable r = () -> {}; // Termina normalmente senza eccezioni
        Consumer<Throwable> handler = ex -> handlerInvoked.set(true);
        
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        assertDoesNotThrow(safeRunnable::run);
        
        // Verifica che handler NON sia stato invocato
        assertFalse(handlerInvoked.get(), "Handler non deve essere invocato per runnable normale");
    }

    @Test
    void testEccezioneChecked() {
        Runnable r = () -> { throw new RuntimeException("checked"); };
        Consumer<Throwable> handler = ex -> {};
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        try {
            safeRunnable.run();
        } catch (RuntimeException e) {
            // Ignora rilancio
        }
    }

    @Test
    void testEccezioneError() {
        Runnable r = () -> { throw new OutOfMemoryError("OOM"); };
        Consumer<Throwable> handler = ex -> {};
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        try {
            safeRunnable.run();
        } catch (OutOfMemoryError e) {
            // Ignora rilancio
        }
    }

    @Test
    void testMessaggioEccezioneNullo() {
        Runnable r = () -> { throw new RuntimeException((String) null); };
        Consumer<Throwable> handler = ex -> assertNull(ex.getMessage());
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        try {
            safeRunnable.run();
        } catch (RuntimeException e) {
            // Ignora rilancio
        }
    }

    @Test
    void testMessaggioEccezioneVuoto() {
        Runnable r = () -> { throw new RuntimeException(""); };
        Consumer<Throwable> handler = ex -> assertEquals("", ex.getMessage());
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        try {
            safeRunnable.run();
        } catch (RuntimeException e) {
            // Ignora rilancio
        }
    }
}
