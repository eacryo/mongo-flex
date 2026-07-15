/*
 * Copyright (c) 2024 mongo-flex contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.github.eacryo.mongoflex.ulid;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * A minimal, zero-dependency ULID (Universally Unique Lexicographically Sortable Identifier)
 * generator conforming to the <a href="https://github.com/ulid/spec">ULID spec</a>.
 *
 * <h3>Binary layout (128 bits)</h3>
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      32_bit_uint_time_high                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     16_bit_uint_time_low      |       16_bit_uint_random      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                       32_bit_uint_random                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                       32_bit_uint_random                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <h3>String encoding</h3>
 * 26 Crockford Base32 characters ({@code 0123456789ABCDEFGHJKMNPQRSTVWXYZ}):
 * 10 timestamp characters + 16 random characters.
 *
 * <h3>Monotonicity</h3>
 * Within the same millisecond, the 80-bit random component is incremented by 1.
 * Clock rollback is handled by reusing the last known timestamp and incrementing the
 * random component (same strategy used by the ulid-creator reference library).
 *
 * <h3>Thread safety</h3>
 * {@link #generate()} is {@code synchronized} on the class — the critical section is
 * only a few hundred nanoseconds of bit manipulation and is self-evidently not a
 * contention point for MongoDB {@code _id} generation.
 *
 * @see <a href="https://github.com/ulid/spec">ULID Specification</a>
 */
public final class Ulid {

    /**
     * Crockford Base32 alphabet — excludes I, L, O, U to avoid visual ambiguity.
     */
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /**
     * Number of bytes in the random component (80 bits = 10 bytes).
     */
    private static final int RANDOM_BYTES = 10;

    /**
     * Total ULID string length.
     */
    private static final int ULID_LENGTH = 26;

    /**
     * Number of timestamp characters.
     */
    private static final int TIMESTAMP_CHARS = 10;

    // ---- monotonic state ----

    private static long lastTimestamp = -1;
    private static final byte[] lastRandom = new byte[RANDOM_BYTES];
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private Ulid() {
        // utility class — no instances
    }

    /**
     * Generate a monotonic, sortable, 26-character ULID string.
     *
     * <p>The generator is thread-safe and handles clock rollback by reusing the
     * last observed timestamp while incrementing the random component.</p>
     *
     * @return a 26-character Crockford Base32 ULID string
     */
    public static synchronized String generate() {
        long now = System.currentTimeMillis();

        // ---- clock rollback guard ----
        // If the system clock moves backwards (NTP adjustment, leap second, etc.),
        // stick with the last known timestamp. This guarantees monotonicity of the
        // timestamp portion under mild clock skew.
        if (now < lastTimestamp) {
            now = lastTimestamp;
        }

        final byte[] random = new byte[RANDOM_BYTES];

        if (now == lastTimestamp) {
            // Same millisecond — increment the 80-bit random component by 1.
            incrementByteArray(lastRandom);
            if (isAllZero(lastRandom)) {
                // 2^80 ULIDs in one millisecond exhausted (practically impossible).
                // Busy-wait until the clock ticks forward.
                while (now <= lastTimestamp) {
                    now = System.currentTimeMillis();
                }
                SECURE_RANDOM.nextBytes(lastRandom);
                lastTimestamp = now;
            }
        } else {
            // New millisecond — fresh random bytes.
            SECURE_RANDOM.nextBytes(lastRandom);
            lastTimestamp = now;
        }

        // Defensive copy — lastRandom is shared state, random is the return snapshot.
        System.arraycopy(lastRandom, 0, random, 0, RANDOM_BYTES);

        return encode(now, random);
    }

    /**
     * Encode timestamp (48 bits) + random (80 bits) into a 26-character Crockford
     * Base32 string.
     */
    static String encode(long timestamp, byte[] random) {
        final char[] chars = new char[ULID_LENGTH];

        // ---- timestamp: 48 bits → 10 characters (50 bits, upper 2 bits always 0) ----
        long t = timestamp;
        for (int i = TIMESTAMP_CHARS - 1; i >= 0; i--) {
            chars[i] = ALPHABET[(int) (t & 0x1F)];
            t >>>= 5;
        }

        // ---- random: 80 bits (10 bytes) → 16 characters ----
        long accumulator = 0;
        int bitsInAccumulator = 0;
        int byteIdx = 0;

        for (int i = TIMESTAMP_CHARS; i < ULID_LENGTH; i++) {
            while (bitsInAccumulator < 5) {
                accumulator = (accumulator << 8) | (random[byteIdx++] & 0xFF);
                bitsInAccumulator += 8;
            }
            bitsInAccumulator -= 5;
            chars[i] = ALPHABET[(int) ((accumulator >> bitsInAccumulator) & 0x1F)];
        }

        return new String(chars);
    }

    /**
     * Increment a 10-byte unsigned big-endian integer in place.  Returns via the
     * mutated array.  Overflow from {@code 0xFF…FF} to {@code 0x00…00} is allowed
     * and detected by the caller.
     */
    private static void incrementByteArray(byte[] bytes) {
        for (int i = bytes.length - 1; i >= 0; i--) {
            // ++ on a signed byte works correctly for unsigned carry:
            //   (byte)0xFF → 0,  0 == 0 ⇒ carry
            //   (byte)0x7F → (byte)0x80,  -128 != 0 ⇒ done
            if (++bytes[i] != 0) {
                break;
            }
        }
    }

    /**
     * Return {@code true} iff every byte in the array is zero.
     */
    private static boolean isAllZero(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    // ---- test-support methods (package-private) ----

    /**
     * Reset internal monotonic state.  Intended <b>only</b> for deterministic tests.
     */
    static synchronized void resetState() {
        lastTimestamp = -1;
        Arrays.fill(lastRandom, (byte) 0);
    }
}
