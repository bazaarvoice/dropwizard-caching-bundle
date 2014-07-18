package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yammer.dropwizard.util.Duration;
import com.yammer.dropwizard.util.Size;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for local, in-memory cache.
 */
public class LocalCacheConfiguration {
    private Optional<Duration> _expire = Optional.absent();
    private Optional<Size> _maximumSize = Optional.absent();

    public Optional<Duration> getExpire() {
        return _expire;
    }

    @JsonProperty
    public void setExpire(Optional<Duration> expire) {
        checkNotNull(expire);
        checkArgument(!expire.isPresent() || expire.get().getQuantity() >= 0, "expire must be >= 0 (value: {})", expire);
        _expire = expire;
    }

    public Optional<Size> getMaximumSize() {
        return _maximumSize;
    }

    @JsonProperty
    public void setMaximumSize(Optional<Size> maximumMemory) {
        checkNotNull(maximumMemory);
        checkArgument(!maximumMemory.isPresent() || maximumMemory.get().getQuantity() >= 0, "maximumMemory must be >= 0 (value: {})", maximumMemory);
        _maximumSize = maximumMemory;
    }

    public Cache<String, CachedResponse> buildCache() {
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();

        if (!_expire.isPresent() && !_maximumSize.isPresent()) {
            cacheBuilder.maximumSize(0);
        } else {
            if (_expire.isPresent()) {
                Duration expire = _expire.get();
                cacheBuilder.expireAfterWrite(expire.getQuantity(), expire.getUnit());
            }

            if (_maximumSize.isPresent()) {
                cacheBuilder
                        .weigher(CachedResponseWeigher.INSTANCE)
                        .maximumWeight(_maximumSize.get().toBytes());
            }
        }

        return cacheBuilder.build();
    }
}
