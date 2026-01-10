/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.bookkeeper.zookeeper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Manual student-style sanity checks for ExponentialBackoffRetryPolicy variants.
 */
public class ExponentialBackoffRetryPolicyManualTest {

    private final long baseBackoffTime = 100L;
    private final int maxRetries = 5;
    private final ExponentialBackoffRetryPolicy policy = new ExponentialBackoffRetryPolicy(baseBackoffTime, maxRetries);
    private final BoundExponentialBackoffRetryPolicy boundPolicy = new BoundExponentialBackoffRetryPolicy(baseBackoffTime, 500L, maxRetries);
    private final ExponentialBackOffWithDeadlinePolicy deadlinePolicy = new ExponentialBackOffWithDeadlinePolicy(baseBackoffTime, 1000L, maxRetries);

    @Test
    void testAllowRetryWithinAndBeyondLimit() {
        // Verifico che entro maxRetries=5 i retry siano permessi
        assertTrue(policy.allowRetry(0, 0)); // primo tentativo
        assertTrue(policy.allowRetry(5, 0)); // ultimo retry permesso
        // Oltre il limite non deve permettere ulteriori tentativi
        assertFalse(policy.allowRetry(6, 0));
    }

    @Test
    void testNextRetryWaitTimeStaysWithinExpectedRange() {
        // Testo che il backoff cresca esponenzialmente ma con jitter entro range ragionevoli
        long wait1 = policy.nextRetryWaitTime(1, 0);
        long wait3 = policy.nextRetryWaitTime(3, 0);
        
        // Per attemptIndex=1, backoff ~= baseBackoffTime * 2^1 = 200 (con jitter max 400)
        assertThat(wait1, greaterThanOrEqualTo(baseBackoffTime));
        assertThat(wait1, lessThanOrEqualTo(baseBackoffTime * 4));
        
        // Per attemptIndex=3, backoff ~= baseBackoffTime * 2^3 = 800 (con jitter max 1600)
        assertThat(wait3, greaterThanOrEqualTo(baseBackoffTime));
        assertThat(wait3, lessThanOrEqualTo(baseBackoffTime * 16));
    }

    @Test
    void testBoundedBackoffClampsAtCap() {
        // Per attemptIndex alto (10), il calcolo esponenziale darebbe valori enormi
        // ma la versione Bounded deve clampare al cap=500ms
        long waitHigh = boundPolicy.nextRetryWaitTime(10, 0);
        assertThat(waitHigh, lessThanOrEqualTo(500L)); // non supera il cap
        assertThat(waitHigh, greaterThanOrEqualTo(baseBackoffTime)); // ma almeno baseBackoffTime
    }

    @Test
    void testDeadlinePolicyStopsAtDeadlineAndClampsWait() {
        // Prima della deadline (1000ms), il retry è permesso
        assertTrue(deadlinePolicy.allowRetry(2, 500));
        // Dopo la deadline non deve permettere retry
        assertFalse(deadlinePolicy.allowRetry(2, 1200));

        // Se mancano 50ms alla deadline, il wait deve essere clampato a 50ms
        // indipendentemente dal backoff calcolato
        long wait = deadlinePolicy.nextRetryWaitTime(4, 950L);
        assertEquals(50L, wait); // 1000 - 950 = 50ms residui
    }
}
