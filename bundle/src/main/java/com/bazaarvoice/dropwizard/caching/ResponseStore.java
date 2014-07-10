package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;

/**
 * Shared response cache.
 */
public abstract class ResponseStore {
    public static final ResponseStore NULL_STORE = new ResponseStore() {
        @Override
        public Optional<CachedResponse> get(String key) {
            return Optional.absent();
        }

        @Override
        public void put(String key, CachedResponse response) {
            // Do nothing
        }

        @Override
        public void invalidate(String key) {
            // Do nothing
        }
    };

    public abstract Optional<CachedResponse> get(String key);

    public abstract void put(String key, CachedResponse response);

    public abstract void invalidate(String key);
}
