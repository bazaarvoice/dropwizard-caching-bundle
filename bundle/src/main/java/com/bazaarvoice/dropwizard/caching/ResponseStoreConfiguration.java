package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;

/**
 * Configuration options for shared cache store.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ResponseStoreConfiguration extends Discoverable {
    public abstract ResponseStore createStore();
}
