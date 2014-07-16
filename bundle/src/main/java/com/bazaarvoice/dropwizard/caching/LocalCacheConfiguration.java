package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yammer.dropwizard.util.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for local, in-memory cache.
 */
public class LocalCacheConfiguration {
    private Optional<Integer> _maximumSize = Optional.absent();
    private Optional<Duration> _expire = Optional.absent();

    public Optional<Integer> getMaximumSize() {
        return _maximumSize;
    }

    @JsonProperty
    public LocalCacheConfiguration maximumSize(Optional<Integer> maximumSize) {
        checkNotNull(maximumSize);
        checkArgument(!maximumSize.isPresent() || maximumSize.get() >= 0, "maximumSize must be >= 0 (value: {})", maximumSize);
        _maximumSize = maximumSize;
        return this;
    }

    public Optional<Duration> getExpire() {
        return _expire;
    }

    @JsonProperty
    public LocalCacheConfiguration expire(Optional<Duration> expire) {
        checkNotNull(expire);
        checkArgument(!expire.isPresent() || expire.get().getQuantity() >= 0, "expire must be >= 0 (value: {})", expire);
        _expire = expire;
        return this;
    }

    public Cache<String, Optional<CachedResponse>> buildCache() {
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();

        if (!_expire.isPresent() && !_maximumSize.isPresent()) {
            // No local cache storage
            return cacheBuilder
                    .maximumSize(0)
                    .build();
        }

        if (_expire.isPresent()) {
            Duration expire = _expire.get();
            cacheBuilder.expireAfterWrite(expire.getQuantity(), expire.getUnit());
        }

        if (_maximumSize.isPresent()) {
            cacheBuilder.maximumSize(_maximumSize.get());
        }

        return cacheBuilder.build();
    }
}
