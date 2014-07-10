package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for {@link CachingBundle}.
 */
public class CachingConfiguration {
    private boolean _enabled = true;
    private LocalCacheConfiguration _local = new LocalCacheConfiguration();
    private Optional<ResponseStoreConfiguration> _store = Optional.absent();

    public boolean isEnabled() {
        return _enabled;
    }

    @JsonProperty
    public CachingConfiguration enabled(boolean enabled) {
        _enabled = enabled;
        return this;
    }

    public LocalCacheConfiguration getLocal() {
        return _local;
    }

    @JsonProperty
    public CachingConfiguration local(LocalCacheConfiguration local) {
        _local = checkNotNull(local);
        return this;
    }

    public Optional<ResponseStoreConfiguration> getStore() {
        return _store;
    }

    @JsonProperty
    public CachingConfiguration store(Optional<ResponseStoreConfiguration> store) {
        _store = checkNotNull(store);
        return this;
    }

    public CachingConfiguration store(ResponseStoreConfiguration store) {
        return store(Optional.of(store));
    }

    public ResponseCache buildCache() {
        Cache<String, Optional<CachedResponse>> localCache = _local.newCacheBuilder().build();
        ResponseStore store = _store.isPresent()
                ? _store.get().createStore()
                : null;

        return new ResponseCache(localCache, Optional.fromNullable(store));
    }
}
