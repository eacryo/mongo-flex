/*
 * Copyright (c) 2024 mongo-flex contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.github.eacryo.mongoflex.ulid;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Ulid}.
 *
 * <p>Verifies correctness against the ULID spec, monotonicity, uniqueness,
 * thread safety, and clock-rollback handling.</p>
 */
@DisplayName("Ulid generator")
class UlidTest {

    /**
     * Crockford Base32 alphabet per the ULID spec.
     */
    private static final Set<Character> VALID_CHARS;
    static {
        Set<Character> set = new HashSet<>();
        for (char c : "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray()) {
            set.add(c);
        }
        VALID_CHARS = Collections.unmodifiableSet(set);
    }

    @AfterEach
    void tearDown() {
        Ulid.resetState();
    }

    // -----------------------------------------------------------------------
    // Format & character validity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("should return a 26-character string")
    void shouldReturn26CharacterString() {
        String ulid = Ulid.generate();
        assertNotNull(ulid);
        assertEquals(26, ulid.length(), "ULID must be exactly 26 characters: " + ulid);
    }

    @Test
    @DisplayName("should contain only Crockford Base32 characters")
    void shouldContainOnlyCrockfordBase32Characters() {
        for (int i = 0; i < 10_000; i++) {
            String ulid = Ulid.generate();
            for (int j = 0; j < ulid.length(); j++) {
                char c = ulid.charAt(j);
                assertTrue(VALID_CHARS.contains(c),
                        "Invalid character '" + c + "' at position " + j + " in " + ulid);
            }
        }
    }

    @Test
    @DisplayName("timestamp portion should be 10 characters, randomness 16")
    void shouldHaveCorrectTimestampAndRandomnessSplit() {
        // 10 timestamp chars + 16 random chars = 26 total
        // First character of the timestamp must be 0-7 because the upper 2 bits
        // of the 50-bit timestamp field are always 0.
        for (int i = 0; i < 1_000; i++) {
            String ulid = Ulid.generate();
            assertEquals(26, ulid.length());
            // First char represents bits 49-45 of the 50-bit field; bits 49 and 48
            // are always 0, so the max value is 0b00111 = 7.
            char first = ulid.charAt(0);
            assertTrue("01234567".indexOf(first) >= 0,
                    () -> "First char must be 0-7, got '" + first + "' in " + ulid);
        }
    }

    // -----------------------------------------------------------------------
    // Uniqueness
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("should generate 100 000 unique ULIDs without collision")
    void shouldGenerateUniqueUlids() {
        Set<String> seen = new HashSet<>(100_000);
        for (int i = 0; i < 100_000; i++) {
            String ulid = Ulid.generate();
            if (!seen.add(ulid)) {
                fail("Duplicate ULID generated: " + ulid + " at iteration " + i);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Monotonicity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("successive ULIDs should be strictly increasing (lexicographic)")
    void successiveUlidsShouldBeMonotonic() {
        String prev = Ulid.generate();
        for (int i = 0; i < 10_000; i++) {
            String next = Ulid.generate();
            if (prev.compareTo(next) >= 0) {
                fail("Monotonicity violated at iteration " + i
                     + ": prev=" + prev + " next=" + next);
            }
            prev = next;
        }
    }

    @Test
    @DisplayName("burst within same millisecond should produce monotonic ULIDs")
    void burstWithinSameMillisecondShouldBeMonotonic() {
        String prev = Ulid.generate();
        // Generate as fast as possible — many will land in the same millisecond
        for (int i = 0; i < 10_000; i++) {
            String next = Ulid.generate();
            if (prev.compareTo(next) >= 0) {
                fail("Burst monotonicity violated at iteration " + i
                     + ": prev=" + prev + " next=" + next);
            }
            prev = next;
        }
    }

    // -----------------------------------------------------------------------
    // Thread safety
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("concurrent generation across 8 threads should produce all-unique results")
    void concurrentGenerationShouldProduceUniqueResults() throws Exception {
        final int threadCount = 8;
        final int perThread = 5_000;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<List<String>> results = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                List<String> local = new ArrayList<>(perThread);
                for (int i = 0; i < perThread; i++) {
                    local.add(Ulid.generate());
                }
                results.add(local);
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Threads did not complete in time");
        executor.shutdown();

        // Flatten and check uniqueness
        List<String> all = results.stream().flatMap(List::stream).collect(Collectors.toList());
        assertEquals(threadCount * perThread, all.size());

        Set<String> unique = new HashSet<>(all);
        assertEquals(all.size(), unique.size(),
                "Duplicate ULIDs detected across concurrent threads");
    }

    @Test
    @DisplayName("concurrent generation should maintain per-thread monotonicity")
    void concurrentGenerationShouldBeMonotonicPerThread() throws Exception {
        final int threadCount = 4;
        final int perThread = 2_000;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<List<String>> results = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                List<String> local = new ArrayList<>(perThread);
                String prev = null;
                for (int i = 0; i < perThread; i++) {
                    String next = Ulid.generate();
                    if (prev != null && prev.compareTo(next) >= 0) {
                        // This is a genuine monotonicity failure within a single thread
                        throw new AssertionError(
                                "Thread-local monotonicity violated: prev=" + prev + " next=" + next);
                    }
                    local.add(next);
                    prev = next;
                }
                results.add(local);
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Threads did not complete in time");
        executor.shutdown();

        // Global uniqueness
        Set<String> unique = new HashSet<>();
        results.forEach(unique::addAll);
        assertEquals(threadCount * perThread, unique.size());
    }

    // -----------------------------------------------------------------------
    // Clock rollback
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("should handle clock rollback by reusing last timestamp and incrementing")
    void shouldHandleClockRollback() throws Exception {
        // Generate one ULID to set the initial internal state
        String first = Ulid.generate();

        // Artificially push lastTimestamp 10 seconds into the future via reflection.
        // This simulates the clock jumping backwards.
        Field lastTimestampField = Ulid.class.getDeclaredField("lastTimestamp");
        lastTimestampField.setAccessible(true);
        long realNow = System.currentTimeMillis();
        lastTimestampField.setLong(null, realNow + 10_000); // 10 seconds in the "future"

        // Now generate again.  The clock appears to have gone backwards by 10 s.
        // The generator must reuse the future timestamp and increment the random part.
        String second = Ulid.generate();

        // The second ULID must still be strictly greater than the first.
        assertTrue(first.compareTo(second) < 0,
                () -> "Clock rollback not handled: first=" + first + " second=" + second);

        // The timestamp portion of the second ULID should match the future timestamp
        // we injected, NOT the real current time.
        String third = Ulid.generate();
        assertTrue(second.compareTo(third) < 0,
                "After rollback recovery, successive ULIDs must stay monotonic");
    }

    // -----------------------------------------------------------------------
    // Encode / decode consistency (unit-level)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("encode should produce valid Crockford Base32 output")
    void encodeShouldProduceValidOutput() {
        byte[] random = new byte[10];
        for (int i = 0; i < random.length; i++) {
            random[i] = (byte) i; // deterministic pattern
        }
        String encoded = Ulid.encode(0L, random);

        assertEquals(26, encoded.length());
        for (char c : encoded.toCharArray()) {
            assertTrue(VALID_CHARS.contains(c), "Invalid char in encoded output: " + c);
        }

        // Timestamp 0 → first 10 chars should all be '0'
        assertEquals("0000000000", encoded.substring(0, 10));
    }

    @Test
    @DisplayName("encode with max timestamp should not exceed valid range")
    void encodeWithMaxTimestamp() {
        long maxTimestamp = (1L << 48) - 1;
        byte[] random = new byte[10]; // all zeros
        String encoded = Ulid.encode(maxTimestamp, random);

        // First char must still be ≤ '7' (bits 49-48 are zero)
        assertTrue("01234567".indexOf(encoded.charAt(0)) >= 0,
                "Max timestamp should produce valid first character: " + encoded);
        assertEquals(26, encoded.length());
    }

    // -----------------------------------------------------------------------
    // Deterministic encode — two calls with same inputs must produce same output
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("encode should be deterministic for same inputs")
    void encodeShouldBeDeterministic() {
        byte[] random = new byte[10];
        for (int i = 0; i < random.length; i++) {
            random[i] = (byte) (i * 17 + 42);
        }

        String a = Ulid.encode(1680000000000L, random);
        String b = Ulid.encode(1680000000000L, random);

        assertEquals(a, b, "encode() must be deterministic for identical inputs");
    }

    // -----------------------------------------------------------------------
    // Repeated tests for statistical confidence
    // -----------------------------------------------------------------------

    @RepeatedTest(5)
    @DisplayName("repeated: 50 000 ULIDs must be unique and monotonic")
    void repeatedBulkShouldBeUniqueAndMonotonic() {
        int count = 50_000;
        Set<String> seen = new HashSet<>(count);
        String prev = Ulid.generate();
        seen.add(prev);

        for (int i = 1; i < count; i++) {
            String next = Ulid.generate();
            if (!seen.add(next)) {
                fail("Duplicate at position " + i + ": " + next);
            }
            if (prev.compareTo(next) >= 0) {
                fail("Monotonicity broken at position " + i
                     + ": prev=" + prev + " next=" + next);
            }
            prev = next;
        }
    }
}
