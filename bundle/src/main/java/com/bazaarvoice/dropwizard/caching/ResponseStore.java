package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;

/**
 * Shared response cache.
 */
public abstract class ResponseStore {
    public abstract Optional<CachedResponse> get(String key);

    public abstract void put(String key, CachedResponse response);
}
