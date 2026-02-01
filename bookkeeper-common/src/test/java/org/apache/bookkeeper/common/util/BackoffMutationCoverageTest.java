/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bookkeeper.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Focused mutation coverage tests for Backoff class targeting
 * critical boundary conditions and math operations.
 */
@DisplayName("Backoff Mutation Coverage Tests")
public class BackoffMutationCoverageTest {

    /**
     * Verify Constant factory produces non-empty streams
     */
    @Test
    @DisplayName("Constant.toBackoffs() must return non-empty Stream")
    public void testConstantToBackoffsNonEmpty() {
        Backoff.Policy policy = Backoff.Constant.of(100, 5);
        List<Long> backoffs = policy.toBackoffs().collect(Collectors.toList());
        
        assertNotNull(backoffs);
        assertFalse(backoffs.isEmpty(), "toBackoffs() should return non-empty Stream");
        assertEquals(5, backoffs.size());
        assertTrue(backoffs.stream().allMatch(v -> v == 100), "All values should be 100ms");
    }

    /**
     * Verify Exponential factory produces exponential growth
     */
    @Test
    @DisplayName("Exponential.toBackoffs() must return non-empty exponential sequence")
    public void testExponentialToBackoffsNonEmpty() {
        Backoff.Policy policy = Backoff.Exponential.of(10, 100, 2, 5);
        List<Long> backoffs = policy.toBackoffs().collect(Collectors.toList());
        
        assertNotNull(backoffs);
        assertFalse(backoffs.isEmpty(), "toBackoffs() should return non-empty Stream");
        assertEquals(5, backoffs.size());
        assertEquals(10L, backoffs.get(0), "First element should be startMs");
        assertTrue(backoffs.get(1) > backoffs.get(0), "Values should increase exponentially");
    }

    /**
     * Verify all factory methods produce working streams
     */
    @Test
    @DisplayName("All factory methods produce working streams")
    public void testAllFactoriesProduceWorkingPolicies() {
        assertEquals(5, Backoff.constant(100).limit(5).count());
        assertEquals(5, Backoff.exponential(10, 2, 500).limit(5).count());
        assertEquals(5, Backoff.exponentialJittered(10, 500).limit(5).count());
        assertEquals(5, Backoff.decorrelatedJittered(10, 500).limit(5).count());
        assertEquals(5, Backoff.equalJittered(10, 500).limit(5).count());
    }

    /**
     * Verify decorrelatedJittered respects maxMs boundary
     */
    @Test
    @DisplayName("decorrelatedJittered must never exceed maxMs")
    public void testDecorrelatedJitteredMaxBoundary() {
        long maxMs = 5000;
        List<Long> values = Backoff.decorrelatedJittered(100, maxMs).limit(20).collect(Collectors.toList());
        
        assertTrue(values.stream().allMatch(v -> v <= maxMs),
            "decorrelatedJittered must never exceed maxMs");
        assertTrue(values.stream().allMatch(v -> v >= 100),
            "decorrelatedJittered must respect startMs");
    }

    /**
     * Verify equalJittered respects boundaries
     */
    @Test
    @DisplayName("equalJittered must respect both startMs and maxMs boundaries")
    public void testEqualJitteredBoundaries() {
        long startMs = 200;
        long maxMs = 8000;
        List<Long> values = Backoff.equalJittered(startMs, maxMs).limit(20).collect(Collectors.toList());
        
        assertTrue(values.stream().allMatch(v -> v >= startMs),
            "equalJittered must never be less than startMs");
        assertTrue(values.stream().allMatch(v -> v <= maxMs),
            "equalJittered must never exceed maxMs");
    }

    /**
     * Verify exponentialJittered preserves startMs boundary
     */
    @Test
    @DisplayName("exponentialJittered first value must equal startMs")
    public void testExponentialJitteredStartBoundary() {
        List<Long> values = Backoff.exponentialJittered(500, 20000).limit(5).collect(Collectors.toList());
        
        assertEquals(500L, values.get(0), "First value must equal startMs");
        
        for (int i = 1; i < values.size(); i++) {
            assertTrue(values.get(i) >= 500, "All values must be >= startMs");
            assertTrue(values.get(i) <= 20000, "All values must be <= maxMs");
        }
    }

    /**
     * Verify decorrelatedJittered with mocked random respects Math.min() boundary
     */
    @Test
    @DisplayName("decorrelatedJittered Math.min() must enforce maxMs cap")
    public void testDecorrelatedJitteredMinBoundary() {
        try (MockedStatic<ThreadLocalRandom> mockedRandom = mockStatic(ThreadLocalRandom.class)) {
            ThreadLocalRandom mockInstance = mock(ThreadLocalRandom.class);
            mockedRandom.when(ThreadLocalRandom::current).thenReturn(mockInstance);
            
            mockedRandom.when(() -> ThreadLocalRandom.current().nextLong(anyLong()))
                .thenReturn(Long.MAX_VALUE - 1);
            
            List<Long> values = Backoff.decorrelatedJittered(100, 500).limit(4).collect(Collectors.toList());
            
            assertTrue(values.stream().allMatch(v -> v <= 500),
                "Math.min() must cap values to maxMs");
        }
    }

    /**
     * Verify equalJittered with mocked extreme values respects maxMs
     */
    @Test
    @DisplayName("equalJittered with deterministic random must respect maxMs boundary")
    public void testEqualJitteredWithMockedRandom() {
        try (MockedStatic<ThreadLocalRandom> mockedRandom = mockStatic(ThreadLocalRandom.class)) {
            ThreadLocalRandom mockInstance = mock(ThreadLocalRandom.class);
            mockedRandom.when(ThreadLocalRandom::current).thenReturn(mockInstance);
            
            mockedRandom.when(() -> ThreadLocalRandom.current().nextLong(anyLong()))
                .thenReturn(Long.MAX_VALUE / 2);
            
            List<Long> values = Backoff.equalJittered(50, 500).limit(5).collect(Collectors.toList());
            
            for (Long value : values) {
                assertTrue(value <= 500,
                    String.format("Value %d exceeds maxMs (500)", value));
            }
        }
    }

    /**
     * Verify decorrelatedJittered with mocked random produces bounded values
     */
    @Test
    @DisplayName("decorrelatedJittered with deterministic random must produce correct values")
    public void testDecorrelatedJitteredWithMockedRandom() {
        try (MockedStatic<ThreadLocalRandom> mockedRandom = mockStatic(ThreadLocalRandom.class)) {
            ThreadLocalRandom mockInstance = mock(ThreadLocalRandom.class);
            mockedRandom.when(ThreadLocalRandom::current).thenReturn(mockInstance);
            
            mockedRandom.when(() -> ThreadLocalRandom.current().nextLong(anyLong())).thenReturn(5000000L);
            
            List<Long> values = Backoff.decorrelatedJittered(100, 1000).limit(3).collect(Collectors.toList());
            
            assertTrue(values.stream().allMatch(v -> v >= 100 && v <= 1000),
                "All values must be within bounds [100, 1000]");
            assertFalse(values.isEmpty(), "Stream should not be empty");
        }
    }

    /**
     * Comprehensive test: verify variance in jitter algorithms
     */
    @Test
    @DisplayName("All jitter algorithms must produce varied values within bounds")
    public void testAllJitterAlgorithmsMustProduceVariedBoundedValues() {
        int iterations = 10;
        
        List<Long> decorrelated = Backoff.decorrelatedJittered(100, 50000)
            .limit(iterations).collect(Collectors.toList());
        assertTrue(decorrelated.stream().allMatch(v -> v >= 100 && v <= 50000),
            "decorrelatedJittered: All values must be in [100, 50000]");
        assertTrue(calculateVariance(decorrelated) > 0, 
            "decorrelatedJittered must have variance");
        
        List<Long> equal = Backoff.equalJittered(1000, 100000)
            .limit(iterations).collect(Collectors.toList());
        assertTrue(equal.stream().allMatch(v -> v >= 1000 && v <= 100000),
            "equalJittered: All values must be in [1000, 100000]");
        assertTrue(calculateVariance(equal) > 0, 
            "equalJittered must have variance");
        
        List<Long> exponential = Backoff.exponentialJittered(100, 50000)
            .limit(iterations).collect(Collectors.toList());
        assertTrue(exponential.stream().allMatch(v -> v >= 100 && v <= 50000),
            "exponentialJittered: All values must be in [100, 50000]");
        assertTrue(calculateVariance(exponential) > 0, 
            "exponentialJittered must have variance");
    }
    
    private double calculateVariance(List<Long> values) {
        if (values.size() <= 1) return 0;
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0);
        double sumSquaredDiffs = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .sum();
        return sumSquaredDiffs / values.size();
    }
}
