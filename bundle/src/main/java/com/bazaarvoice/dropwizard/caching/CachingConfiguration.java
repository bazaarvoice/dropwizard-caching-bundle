package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for {@link CachingBundle}.
 */
public class CachingConfiguration {
    private LocalCacheConfiguration _local = new LocalCacheConfiguration();
    private ResponseStoreConfiguration _store = new ResponseStoreConfiguration() {
        @Override
        public ResponseStore createStore() {
            return ResponseStore.NULL_STORE;
        }
    };

    public LocalCacheConfiguration getLocal() {
        return _local;
    }

    @JsonProperty
    public CachingConfiguration local(LocalCacheConfiguration local) {
        _local = checkNotNull(local);
        return this;
    }

    public ResponseStoreConfiguration getStore() {
        return _store;
    }

    @JsonProperty
    public CachingConfiguration store(ResponseStoreConfiguration store) {
        _store = checkNotNull(store);
        return this;
    }

    public ResponseCache buildCache() {
        Cache<String, Optional<CachedResponse>> localCache = _local.newCacheBuilder().build();
        return new ResponseCache(localCache, Optional.fromNullable(_store.createStore()));
    }
}
