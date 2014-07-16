package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yammer.dropwizard.util.Duration;
import com.yammer.dropwizard.util.Size;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Configuration options for local, in-memory cache.
 */
public class LocalCacheConfiguration {
    private Optional<Integer> _maximumSize = Optional.absent();
    private Optional<Duration> _expire = Optional.absent();
    private Optional<Size> _maximumMemory = Optional.absent();

    public Optional<Integer> getMaximumSize() {
        return _maximumSize;
    }

    @JsonProperty
    public void setMaximumSize(Optional<Integer> maximumSize) {
        checkNotNull(maximumSize);
        checkArgument(!maximumSize.isPresent() || maximumSize.get() >= 0, "maximumSize must be >= 0 (value: {})", maximumSize);
        checkState(!_maximumMemory.isPresent() || !maximumSize.isPresent());
        _maximumSize = maximumSize;
    }

    public Optional<Duration> getExpire() {
        return _expire;
    }

    @JsonProperty
    public void setExpire(Optional<Duration> expire) {
        checkNotNull(expire);
        checkArgument(!expire.isPresent() || expire.get().getQuantity() >= 0, "expire must be >= 0 (value: {})", expire);
        _expire = expire;
    }

    public Optional<Size> getMaximumMemory() {
        return _maximumMemory;
    }

    @JsonProperty
    public void setMaximumMemory(Optional<Size> maximumMemory) {
        checkNotNull(maximumMemory);
        checkArgument(!maximumMemory.isPresent() || maximumMemory.get().getQuantity() >= 0, "maximumMemory must be >= 0 (value: {})", maximumMemory);
        checkState(!maximumMemory.isPresent() || !_maximumSize.isPresent());
        _maximumMemory = maximumMemory;
    }

    public Cache<String, Optional<CachedResponse>> buildCache() {
        if (!_expire.isPresent() && !_maximumSize.isPresent() && !_maximumMemory.isPresent()) {
            // No local cache storage
            return CacheBuilder.newBuilder()
                    .maximumSize(0)
                    .build();
        }

        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();

        if (_expire.isPresent()) {
            Duration expire = _expire.get();
            cacheBuilder.expireAfterWrite(expire.getQuantity(), expire.getUnit());
        }

        if (_maximumSize.isPresent()) {
            cacheBuilder.maximumSize(_maximumSize.get());
        } else if (_maximumMemory.isPresent()) {
            cacheBuilder
                    .weigher(CachedResponseWeigher.INSTANCE)
                    .maximumWeight(_maximumMemory.get().toBytes());
        }

        return cacheBuilder.build();
    }
}
