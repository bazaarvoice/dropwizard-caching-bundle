/*
 * Copyright 2014 Bazaarvoice, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bazaarvoice.dropwizard.caching.memcached;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.sun.jersey.core.util.Base64;
import net.spy.memcached.MemcachedClientIF;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Memcached key utility functions.
 */
public class KeyUtils {
    /**
     * Length of a base 64 encoded SHA-1 hash in characters.
     */
    private static final int HASH_LENGTH = 28;
    /**
     * Last char in a base 64 encoded SHA-1 hash.
     */
    private static final char LAST_HASH_CHAR = '=';
    /**
     * Maximum allowed length of a memcached key, in bytes.
     */
    private static final int MAX_KEY_BYTES = MemcachedClientIF.MAX_KEY_LENGTH;
    /**
     * Number of bytes to truncate the key at to accommodate they hash as a suffix.
     */
    private static final int KEY_TRUNCATE_BYTES = MAX_KEY_BYTES - HASH_LENGTH;

    /**
     * This method truncates keys that exceed the limit for memcached while attempting to keep them
     * unique. The key is truncated and a hash of the original key is appended.
     */
    public static String truncateKey(String key) {
        byte[] keyBytes = key.getBytes(Charsets.UTF_8);

        if (keyBytes.length < MAX_KEY_BYTES || (keyBytes.length == MAX_KEY_BYTES && keyBytes[keyBytes.length - 1] != LAST_HASH_CHAR)) {
            // Key is short enough
            // If the key ends in equal sign, it could conflict with a truncated/hashed key, so
            // truncate and hash it
            return key;
        }

        // Generate hash of original, full key
        byte[] keyHash = generateKeyHash(keyBytes);
        assert keyHash.length == HASH_LENGTH;
        assert keyHash[keyHash.length - 1] == LAST_HASH_CHAR;

        // Overwrite ending partial character with single byte chars
        // This is not strictly required, but keeps the key valid UTF-8
        int lastCharIndex = KEY_TRUNCATE_BYTES - 1;

        while ((keyBytes[lastCharIndex] & 0xC0) == 0x80) {
            lastCharIndex -= 1;
        }

        if (utf8CharLen(keyBytes[lastCharIndex]) > (KEY_TRUNCATE_BYTES - lastCharIndex)) {
            while (lastCharIndex < KEY_TRUNCATE_BYTES) {
                keyBytes[lastCharIndex++] = '+';
            }
        }

        // Append key hash
        System.arraycopy(keyHash, 0, keyBytes, KEY_TRUNCATE_BYTES, keyHash.length);
        return new String(keyBytes, 0, MAX_KEY_BYTES, Charsets.UTF_8);
    }

    private static byte[] generateKeyHash(byte[] keyBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(keyBytes);
            return Base64.encode(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-1 is included with every java distro, so this exception should not occur
            throw Throwables.propagate(ex);
        }
    }

    private static int utf8CharLen(byte byte1) {
        if ((byte1 & 0x80) == 0) {
            return 1;
        } else if ((byte1 & 0xE0) == 0xC0) {
            return 2;
        } else if ((byte1 & 0xF0) == 0xE0) {
            return 3;
        } else if ((byte1 & 0xF8) == 0xF0) {
            return 4;
        } else if ((byte1 & 0xFC) == 0xF8) {
            return 5;
        } else {
            return 6;
        }
    }
}
