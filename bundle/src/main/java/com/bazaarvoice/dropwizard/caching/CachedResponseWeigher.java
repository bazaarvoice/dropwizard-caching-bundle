package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;
import com.google.common.cache.Weigher;

/**
 * Weigher that attempts to estimate the size of the cached response in bytes.
 */
public class CachedResponseWeigher implements Weigher<String, Optional<CachedResponse>> {
    private static final int HEADER_OVERHEAD_BYTES = 100;
    public static final CachedResponseWeigher INSTANCE = new CachedResponseWeigher();

    private CachedResponseWeigher() {
        // Private constructor to prevent instances being created
    }

    @Override
    public int weigh(String key, Optional<CachedResponse> value) {
        return (key.length() * 2) +
                (value.isPresent()
                        ? value.get().getResponseContent().length + HEADER_OVERHEAD_BYTES
                        : 0);
    }
}
