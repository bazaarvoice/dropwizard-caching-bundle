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
package com.bazaarvoice.dropwizard.caching;

import com.google.common.cache.Weigher;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * Weigher that attempts to estimate the size of the cached response in bytes.
 */
public class CachedResponseWeigher implements Weigher<String, CachedResponse> {
    private static final int CHAR_BYTES = 2;
    public static final CachedResponseWeigher INSTANCE = new CachedResponseWeigher();

    private CachedResponseWeigher() {
        // Private constructor to prevent instances being created
    }

    @Override
    public int weigh(@Nonnull String key, @Nonnull CachedResponse value) {
        int weight = 0;

        // This just an estimate for weighing purposes, not a precise calculation of memory use.

        // Size of the key
        weight += key.length() * CHAR_BYTES;

        // Size of the headers
        for (Map.Entry<String, List<String>> header : value.getResponseHeaders().entrySet()) {
            weight += header.getKey().length() * CHAR_BYTES;

            for (String headerValue : header.getValue()) {
                weight += headerValue.length() * CHAR_BYTES;
            }
        }

        // Size of the body
        weight += value.getResponseContent().length;

        return weight;
    }
}
