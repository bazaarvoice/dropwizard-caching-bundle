package com.bazaarvoice.dropwizard.caching;

import com.bazaarvoice.dropwizard.caching.memcached.MemcachedResponseStoreFactory;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Configuration options for shared cache store.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(MemcachedResponseStoreFactory.class)})
public interface ResponseStoreFactory {
    public static final ResponseStoreFactory NULL_STORE_FACTORY = new ResponseStoreFactory() {
        @Override
        public ResponseStore createStore() {
            return ResponseStore.NULL_STORE;
        }
    };

    public abstract ResponseStore createStore();
}
