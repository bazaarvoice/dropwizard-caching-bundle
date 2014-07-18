package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricsRegistry;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class ResponseCache {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseCache.class);

    private final LocalCache _localCache;
    private final ResponseStore _store;
    private final Counter _hits;
    private final Counter _misses;

    public ResponseCache(Cache<String, CachedResponse> localCache, ResponseStore store, MetricsRegistry metricsRegistry) {
        checkNotNull(localCache, "localCache");
        checkNotNull(store, "store");
        checkNotNull(metricsRegistry, "metricsRegistry");

        _localCache = new LocalCache(localCache, metricsRegistry);
        _store = failTrap(store, metricsRegistry);

        _hits = newCounter(metricsRegistry, "hits");
        _misses = newCounter(metricsRegistry, "misses");
    }

    public Optional<Response> get(CacheRequestContext request) {
        // If request allows a cached response to be returned
        if (isServableFromCache(request)) {
            String cacheKey = buildKey(request);
            StoreLoader loader = new StoreLoader(_store, cacheKey);
            CachedResponse cachedResponse = _localCache.get(cacheKey, loader);

            if (cachedResponse != null && cachedResponse.hasExpiration()) {
                DateTime now = DateTime.now();

                // If cached response is acceptable for request cache control options
                if (isCacheAcceptable(request, now, cachedResponse)) {
                    return buildResponse(request, cacheKey, cachedResponse, now);
                } else if (!loader.invoked && cachedResponse.isExpired(now)) {
                    // Check if the backing store has a fresher copy of the response

                    _localCache.invalidate(cacheKey);
                    cachedResponse = _localCache.get(cacheKey, loader);

                    if (cachedResponse != null && cachedResponse.hasExpiration() && isCacheAcceptable(request, now, cachedResponse)) {
                        return buildResponse(request, cacheKey, cachedResponse, now);
                    }
                }
            }
        }

        _misses.inc();

        if (isOnlyCacheAllowed(request)) {
            return Optional.of(Response.status(HttpUtils.GATEWAY_TIMEOUT).build());
        } else {
            return Optional.absent();
        }
    }

    private Optional<Response> buildResponse(CacheRequestContext request, String cacheKey, CachedResponse response, DateTime now) {
        // If request specifies that response MUST NOT be cached
        if (!isResponseCacheable(request)) {
            _store.invalidate(cacheKey);
            _localCache.invalidate(cacheKey);
        }

        _hits.inc();

        return Optional.of(response.response(now).build());
    }

    public void put(CacheRequestContext request, CacheResponseContext response, byte[] content) {
        if (isResponseCacheable(request) && isResponseCacheable(response)) {
            DateTime responseDate = response.getDate().orNull();

            if (responseDate == null) {
                responseDate = DateTime.now();
                response.setDate(responseDate);
                response.setAge(0);
            } else {
                response.setAge(responseDate, DateTime.now());
            }

            response.setExpires(responseDate.plusSeconds(response.getSharedCacheMaxAge()));

            CachedResponse cachedResponse = CachedResponse.build(response.getStatusCode(), response.getHttpContext().getHttpHeaders(), content);
            String cacheKey = buildKey(request);

            _localCache.put(cacheKey, cachedResponse);
            _store.put(cacheKey, cachedResponse);
        }
    }

    private static String buildKey(CacheRequestContext request) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(request.getRequestMethod());
        buffer.append(' ');

        URI requestUri = request.getRequestUri();
        String query = requestUri.getRawQuery();

        buffer.append(requestUri.getRawPath());

        if (!isNullOrEmpty(query)) {
            buffer.append('?').append(query);
        }

        buffer.append('#').append(request.getRequestHash());
        return buffer.toString();
    }

    /**
     * False if the response MUST NOT be served from the cache and the request must be re-validated with the origin
     * server. True if the response may be served from the cache if all other request options allow it.
     *
     * @param request the request context
     * @return true if the response can be served from the cache (assuming other cache options allow it), false if the
     * response must not be served from the cache
     */
    private static boolean isServableFromCache(CacheRequestContext request) {
        RequestCacheControl cacheControl = request.getCacheControl();
        return !cacheControl.isNoCache() && cacheControl.getMaxAge() != 0 && !request.isPragmaNoCache();
    }

    /**
     * True if only a cached response can be served and the request should not be forwarded to the origin service.
     *
     * @param request the request context
     * @return true if only a cached response can be served
     */
    private static boolean isOnlyCacheAllowed(CacheRequestContext request) {
        return request.getCacheControl().isOnlyIfCached();
    }

    /**
     * False if the response to the request MUST NOT be stored in a cache and must be removed from the cache if it
     * exists. True if the response may be cached if all other request options allow it.
     *
     * @param request the request context
     * @return true if the response may be cached, false if the response must not be cached
     */
    private static boolean isResponseCacheable(CacheRequestContext request) {
        return !request.getCacheControl().isNoStore();
    }

    /**
     * False if the response to the request MUST NOT be stored in a cache. True if the response may be cached if all
     * other request options allow it.
     *
     * @param response the response context
     * @return true if the response may be cached, false if the response must not be cached
     */
    private static boolean isResponseCacheable(CacheResponseContext response) {
        CacheControl cacheControl = response.getCacheControl();

        return !cacheControl.isNoStore() &&
                !cacheControl.isNoCache() &&
                !cacheControl.isPrivate() &&
                response.getSharedCacheMaxAge() > 0;
    }

    /**
     * Test if this request allows a specific cached response to be returned.
     *
     * @param request  the request context
     * @param now      instant that represents the current time
     * @param response response to check
     * @return true if the cached response can be returned, false if the request must be re-validated with the origin
     * server
     */
    private static boolean isCacheAcceptable(CacheRequestContext request, DateTime now, CachedResponse response) {
        // NOTE: Do not check that the expiration time is before NOW here. That is verified later against the max-stale
        // cache-control option.
        DateTime responseDate = response.getDate();
        DateTime responseExpires = response.getExpires().get();

        if (responseExpires.isBefore(responseDate)) {
            return false;
        }

        RequestCacheControl requestCacheControl = request.getCacheControl();

        if (requestCacheControl.getMaxAge() > 0) {
            int age = Seconds.secondsBetween(responseDate, now).getSeconds();

            if (age > requestCacheControl.getMaxAge()) {
                return false;
            }
        }

        if (requestCacheControl.getMinFresh() >= 0 || requestCacheControl.getMaxStale() >= 0) {
            int freshness = Seconds.secondsBetween(now, responseExpires).getSeconds();

            if (requestCacheControl.getMinFresh() >= 0 && freshness < requestCacheControl.getMinFresh()) {
                return false;
            }

            if (requestCacheControl.getMaxStale() >= 0) {
                CacheControl responseCacheControl = response.getCacheControl().orNull();
                boolean responseMustRevalidate = responseCacheControl != null && (responseCacheControl.isProxyRevalidate() || responseCacheControl.isMustRevalidate());

                if (!responseMustRevalidate) {
                    return freshness >= -requestCacheControl.getMaxStale();
                }
            }
        }

        return !responseExpires.isBefore(now);
    }

    private static class StoreLoader implements Callable<CachedResponse> {
        boolean invoked;
        final ResponseStore store;
        final String key;

        public StoreLoader(ResponseStore store, String key) {
            this.store = store;
            this.key = key;
        }

        @Override
        public CachedResponse call() throws Exception {
            if (invoked) {
                return null;
            }

            this.invoked = true;
            Optional<CachedResponse> response = this.store.get(this.key);

            if (!response.isPresent()) {
                throw new CacheKeyNotFoundException();
            }

            return response.get();
        }
    }

    private static Counter newCounter(MetricsRegistry registry, String name) {
        return registry.newCounter(ResponseCache.class, name);
    }

    private static class CacheKeyNotFoundException extends RuntimeException {
    }

    /**
     * Wrap the given store so that any exceptions for store methods are logged with the given logger and not
     * propagated. If the store is absent, {@link ResponseStore#NULL_STORE} is returned.
     */
    private static ResponseStore failTrap(ResponseStore store, MetricsRegistry metricsRegistry) {
        if (store == ResponseStore.NULL_STORE) {
            return ResponseStore.NULL_STORE;
        } else {
            return new FailTrap(store, metricsRegistry);
        }
    }

    private static class FailTrap extends ResponseStore {
        private final Counter _hits;
        private final Counter _misses;
        private final Counter _exceptions;
        private final Counter _puts;
        private final Counter _evictions;
        private final ResponseStore _delegate;

        public FailTrap(ResponseStore delegate, MetricsRegistry metricsRegistry) {
            _delegate = checkNotNull(delegate);

            _hits = newCounter(metricsRegistry, "store-hits");
            _misses = newCounter(metricsRegistry, "store-misses");
            _exceptions = newCounter(metricsRegistry, "store-exceptions");
            _puts = newCounter(metricsRegistry, "store-puts");
            _evictions = newCounter(metricsRegistry, "store-evictions");
        }

        @Override
        public Optional<CachedResponse> get(String key) {
            try {
                Optional<CachedResponse> result = _delegate.get(key);

                if (result.isPresent()) {
                    _hits.inc();
                } else {
                    _misses.inc();
                }

                return result;
            } catch (Exception ex) {
                LOG.warn("Response cache store get operation failed: key={}", key, ex);
                _exceptions.inc();
                return Optional.absent();
            }
        }

        @Override
        public void put(String key, CachedResponse response) {
            try {
                _delegate.put(key, response);
                _puts.inc();
            } catch (Exception ex) {
                LOG.warn("Response cache store put operation failed: key={}, response={}", key, response, ex);
                _exceptions.inc();
            }
        }

        @Override
        public void invalidate(String key) {
            try {
                _delegate.invalidate(key);
                _evictions.inc();
            } catch (Exception ex) {
                LOG.warn("Response cache store invalidation operation failed: key={}", key, ex);
                _exceptions.inc();
            }
        }
    }

    private static final class LocalCache {
        private final Cache<String, CachedResponse> _delegate;

        private final Counter _hits;
        private final Counter _misses;
        private final Counter _evictions;

        public LocalCache(Cache<String, CachedResponse> delegate, MetricsRegistry metricsRegistry) {
            _delegate = checkNotNull(delegate);

            _hits = newCounter(metricsRegistry, "local-hits");
            _misses = newCounter(metricsRegistry, "local-misses");
            _evictions = newCounter(metricsRegistry, "local-evictions");

            metricsRegistry.newGauge(ResponseCache.class, "local-count", new Gauge<Long>() {
                @Override
                public Long value() {
                    return _delegate.size();
                }
            });
        }

        public void invalidate(String key) {
            _delegate.invalidate(key);
            _evictions.inc();
        }

        public CachedResponse get(String key, StoreLoader loader) {
            CachedResponse response;

            try {
                response = _delegate.get(key, loader);
            } catch (CacheKeyNotFoundException ex) {
                response = null;
            } catch (Throwable ex) {
                LOG.warn("Failed to load response from cache: key={}", key, ex);
                response = null;
            }

            if (response == null) {
                _misses.inc();
            } else if (!loader.invoked) {
                _hits.inc();
            }

            return response;
        }

        public void put(String key, CachedResponse response) {
            _delegate.put(key, response);
        }
    }
}
