package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;

/**
 * Configuration options for shared cache store.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ResponseStoreFactory extends Discoverable {
    public static final ResponseStoreFactory NULL_STORE_FACTORY = new ResponseStoreFactory() {
        @Override
        public ResponseStore createStore() {
            return ResponseStore.NULL_STORE;
        }
    };

    public abstract ResponseStore createStore();
}
