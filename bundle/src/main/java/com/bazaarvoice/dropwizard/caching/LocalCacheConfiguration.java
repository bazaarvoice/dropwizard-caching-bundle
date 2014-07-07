package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilder;
import io.dropwizard.util.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for local, in-memory cache.
 */
public class LocalCacheConfiguration {
    private int _maximumSize;
    private Duration _expire = Duration.milliseconds(0);

    public int getMaximumSize() {
        return _maximumSize;
    }

    @JsonProperty
    public LocalCacheConfiguration maximumSize(int maximumSize) {
        checkArgument(maximumSize >= 0, "maximumSize must be >= 0 (value: {})", maximumSize);
        _maximumSize = maximumSize;
        return this;
    }

    public Duration getExpire() {
        return _expire;
    }

    @JsonProperty
    public LocalCacheConfiguration expire(Duration expire) {
        checkNotNull(expire);
        checkArgument(expire.getQuantity() >= 0, "expire must be >= 0 (value: {})", expire);
        _expire = expire;
        return this;
    }

    public CacheBuilder<Object, Object> newCacheBuilder() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(_expire.getQuantity(), _expire.getUnit())
                .maximumSize(_maximumSize);
    }
}
