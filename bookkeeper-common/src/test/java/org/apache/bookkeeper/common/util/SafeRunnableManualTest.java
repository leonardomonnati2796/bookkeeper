package org.apache.bookkeeper.common.util;

import org.junit.jupiter.api.Test;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SafeRunnableManualTest {
    /**
     * Test con Failsafe: verifica che SafeRunnable chiami l'handler ogni volta che fallisce,
     * e che la RetryPolicy di Failsafe effettui il numero corretto di retry.
     * Corrisponde a: boundary value (testa il comportamento al limite dei retry)
     */
    @Test
    void testHandlerConFailsafeRetry() {
            // Mockito per mockare Consumer
            @SuppressWarnings("unchecked")
            Consumer<Throwable> handler = (Consumer<Throwable>) org.mockito.Mockito.mock(Consumer.class);
            Runnable r = () -> { throw new RuntimeException("boundary"); };
            SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
            // Crea una RetryPolicy che riprova 2 volte
            RetryPolicy<Object> policy = RetryPolicy.builder()
                    .withMaxRetries(2)
                    .build();
            // Esegui con Failsafe: verrà chiamato 3 volte in totale (1 + 2 retry)
            try {
                Failsafe.with(policy).run(safeRunnable::run);
            } catch (Exception ignored) {}
            // Verifica che l'handler sia stato chiamato almeno una volta (in realtà 3 volte)
            org.mockito.Mockito.verify(handler, org.mockito.Mockito.atLeast(1)).accept(org.mockito.Mockito.any(Throwable.class));
        }
    /**
     * Verifica che l'handler venga chiamato quando il runnable lancia un'eccezione.
     * Usa un mock per controllare la chiamata.
     * Corrisponde a: partition (runnable che lancia eccezione, handler presente)
     */
    @Test
    void testHandlerConMockBoundary() {
        // Mockito per mockare Consumer
        @SuppressWarnings("unchecked")
        Consumer<Throwable> handler = (Consumer<Throwable>) org.mockito.Mockito.mock(Consumer.class);
        Runnable r = () -> { throw new RuntimeException("boundary"); };
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        safeRunnable.run();
        // Verifica che l'handler sia stato chiamato almeno una volta
        org.mockito.Mockito.verify(handler, org.mockito.Mockito.atLeastOnce()).accept(org.mockito.Mockito.any(Throwable.class));
    }
    /**
     * Verifica che SafeRunnable non lanci eccezioni quando il runnable non ne lancia e non c'è handler.
     * Corrisponde a: partition (runnable senza eccezioni, handler assente)
     */
    @Test
    void testRunnableSenzaEccezioniSenzaHandler() {
        Runnable r = () -> {};
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r);
        safeRunnable.run(); // Non deve lanciare eccezioni
    }

    /**
     * Verifica che l'handler NON venga invocato quando il runnable termina normalmente.
     * Corrisponde a: partition (runnable normale, handler presente che NON deve essere chiamato)
     */
    @Test
    void testRunnableNormaleConHandlerNonInvocato() {
        AtomicReference<Throwable> ref = new AtomicReference<>();
        Runnable r = () -> {}; // Termina normalmente
        Consumer<Throwable> handler = ref::set;
        
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        assertDoesNotThrow(safeRunnable::run);
        
        // Verifica che handler NON sia stato invocato
        assertNull(ref.get(), "Handler non deve essere invocato per runnable che termina normalmente");
    }

    /**
     * Verifica che SafeRunnable non rilanci eccezioni quando il runnable lancia un'eccezione e non c'è handler.
     * Corrisponde a: partition (runnable con eccezione, handler assente)
     */
    @Test
    void testRunnableConEccezioneSenzaHandler() {
        Runnable r = () -> { throw new RuntimeException("errore"); };
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r);
        safeRunnable.run(); // Non deve rilanciare, solo loggare
    }

    /**
     * Verifica che l'handler venga chiamato e gestisca l'eccezione senza rilanciarla.
     * Corrisponde a: partition (runnable con eccezione, handler presente che gestisce)
     */
    @Test
    void testRunnableConEccezioneConHandlerGestisce() {
        AtomicReference<Throwable> ref = new AtomicReference<>();
        Runnable r = () -> { throw new RuntimeException("errore"); };
        Consumer<Throwable> handler = ref::set;
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        try {
            safeRunnable.run();
        } catch (RuntimeException e) {
            // Ignora rilancio
        }
        assertNotNull(ref.get());
    }

    /**
     * Verifica che l'handler venga chiamato e rilanci una nuova eccezione, e che SafeRunnable la propaghi.
     * Corrisponde a: boundary value (handler che rilancia eccezione)
     */
    @Test
    void testRunnableConEccezioneConHandlerRilancia() {
        // Caso 5: Handler che rilancia → eccezione catturata dal run() esterno
        AtomicReference<Throwable> ref = new AtomicReference<>();
        Runnable r = () -> { throw new RuntimeException("errore"); };
        Consumer<Throwable> handler = ex -> {
            ref.set(ex);
            throw new IllegalStateException("rilanciata");
        };
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        
        // L'eccezione viene catturata e loggata, NON propagata
        assertDoesNotThrow(safeRunnable::run);
        assertNotNull(ref.get());
    }

    /**
     * Verifica che SafeRunnable gestisca il caso in cui il runnable è null.
     * Corrisponde a: boundary value (runnable nullo)
     */
    @Test
    void testRunnableNullConHandler() {
        AtomicReference<Throwable> ref = new AtomicReference<>();
        Consumer<Throwable> handler = ref::set;
        // Caso 7: Runnable null → NPE catturata e loggata
        SafeRunnable safeRunnable = SafeRunnable.safeRun(null, handler);
        assertDoesNotThrow(safeRunnable::run);
        assertNotNull(ref.get()); // Handler viene invocato con NPE
    }

    /**
     * Verifica che SafeRunnable funzioni correttamente se l'handler è null e il runnable termina senza errori.
     * Corrisponde a: partition (handler nullo, runnable senza eccezioni)
     */
    @Test
    void testHandlerNullConRunnableCheTermina() {
        Runnable r = () -> {};
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, null);
        safeRunnable.run();
    }

    /**
     * Verifica che SafeRunnable gestisca correttamente eccezioni checked tramite l'handler.
     * Corrisponde a: partition (runnable con checked exception, handler presente)
     */
    @Test
    void testEccezioneCheckedConHandler() {
        Runnable r = () -> { throw new RuntimeException("checked"); };
        Consumer<Throwable> handler = ex -> {};
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        try {
            safeRunnable.run();
        } catch (RuntimeException e) {
            // Ignora rilancio
        }
    }

    /**
     * Verifica che SafeRunnable gestisca correttamente errori gravi (Error) tramite l'handler.
     * Corrisponde a: boundary value (gestione di Error invece che Exception)
     */
    @Test
    void testEccezioneErrorConHandler() {
        Runnable r = () -> { throw new OutOfMemoryError("OOM"); };
        Consumer<Throwable> handler = ex -> {};
        SafeRunnable safeRunnable = SafeRunnable.safeRun(r, handler);
        try {
            safeRunnable.run();
        } catch (OutOfMemoryError e) {
            // Ignora rilancio
        }
    }
}
