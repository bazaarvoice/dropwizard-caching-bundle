package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for {@link CachingBundle}.
 */
public class CachingConfiguration {
    private LocalCacheConfiguration _local = new LocalCacheConfiguration();
    private ResponseStoreFactory _storeFactory = ResponseStoreFactory.NULL_STORE_FACTORY;

    public LocalCacheConfiguration getLocal() {
        return _local;
    }

    @JsonProperty
    public void setLocal(LocalCacheConfiguration local) {
        _local = checkNotNull(local);
    }

    public ResponseStoreFactory getStoreFactory() {
        return _storeFactory;
    }

    @JsonProperty("store")
    public void setStoreFactory(ResponseStoreFactory storeFactory) {
        _storeFactory = checkNotNull(storeFactory);
    }

    public ResponseCache buildCache() {
        return new ResponseCache(_local.buildCache(), _storeFactory.createStore());
    }
}
