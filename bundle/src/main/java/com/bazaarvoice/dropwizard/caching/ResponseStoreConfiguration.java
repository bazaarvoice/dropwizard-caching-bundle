package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Configuration options for shared cache store.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class ResponseStoreConfiguration {
    private boolean _enabled = true;

    public boolean isEnabled() {
        return _enabled;
    }

    @JsonProperty
    public ResponseStoreConfiguration enabled(boolean enabled) {
        _enabled = enabled;
        return this;
    }

    public abstract ResponseStore createStore();
}
