package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class ResponseCache {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseCache.class);

    private final Cache<String, Optional<CachedResponse>> _localCache;
    private final ResponseStore _store;

    public ResponseCache(Cache<String, Optional<CachedResponse>> localCache, ResponseStore store) {
        _localCache = checkNotNull(localCache);
        _store = failTrap(checkNotNull(store));
    }

    public Optional<Response> get(CacheRequestContext request) {
        // If request allows a cached response to be returned
        if (isServableFromCache(request)) {
            String cacheKey = buildKey(request);
            StoreLoader loader = new StoreLoader(_store, cacheKey);
            CachedResponse cachedResponse = loadResponse(cacheKey, loader);

            if (cachedResponse != null && cachedResponse.hasExpiration()) {
                DateTime now = DateTime.now();

                // If cached response is acceptable for request cache control options
                if (isCacheAcceptable(request, now, cachedResponse)) {
                    return buildResponse(request, cacheKey, cachedResponse, now);
                } else if (!loader.invoked && cachedResponse.isExpired(now)) {
                    // Check if the backing store has a fresher copy of the response

                    _localCache.invalidate(cacheKey);
                    cachedResponse = loadResponse(cacheKey, loader);

                    if (cachedResponse != null && cachedResponse.hasExpiration() && isCacheAcceptable(request, now, cachedResponse)) {
                        return buildResponse(request, cacheKey, cachedResponse, now);
                    }
                }
            }
        }

        if (isOnlyCacheAllowed(request)) {
            return Optional.of(Response.status(HttpUtils.GATEWAY_TIMEOUT).build());
        }

        return Optional.absent();
    }

    private Optional<Response> buildResponse(CacheRequestContext request, String cacheKey, CachedResponse response, DateTime now) {
        // If request specifies that response MUST NOT be cached
        if (!isResponseCacheable(request)) {
            _store.invalidate(cacheKey);
            _localCache.invalidate(cacheKey);
        }

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

            _localCache.put(cacheKey, Optional.of(cachedResponse));
            _store.put(cacheKey, cachedResponse);
        }
    }

    /**
     * Load the response from the local guava cache (which loads from the response store if necessary).
     */
    private CachedResponse loadResponse(String key, StoreLoader loader) {
        try {
            Optional<CachedResponse> response = _localCache.get(key, loader);

            return response == null
                    ? null
                    : response.orNull();
        } catch (ExecutionException ex) {
            // Since the store is wrapped up so exceptions are swallowed, there should be no way for this exception to
            // occur.
            LOG.warn("Failed to load response from cache: key={}", key, ex);
            return null;
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

    private static class StoreLoader implements Callable<Optional<CachedResponse>> {
        boolean invoked;
        final ResponseStore store;
        final String key;

        public StoreLoader(ResponseStore store, String key) {
            this.store = store;
            this.key = key;
        }

        @Override
        public Optional<CachedResponse> call() throws Exception {
            if (invoked) {
                return Optional.absent();
            }

            this.invoked = true;
            return this.store.get(this.key);
        }
    }

    /**
     * Wrap the given store so that any exceptions for store methods are logged with the given logger and not
     * propagated. If the store is absent, {@link ResponseStore#NULL_STORE} is returned.
     */
    private static ResponseStore failTrap(ResponseStore store) {
        if (store == ResponseStore.NULL_STORE) {
            return ResponseStore.NULL_STORE;
        } else {
            return new FailTrap(store);
        }
    }

    private static class FailTrap extends ResponseStore {
        private final ResponseStore _delegate;

        public FailTrap(ResponseStore delegate) {
            _delegate = checkNotNull(delegate);
        }

        @Override
        public Optional<CachedResponse> get(String key) {
            try {
                return _delegate.get(key);
            } catch (Exception ex) {
                LOG.warn("Response cache store get operation failed: key={}", key, ex);
                return Optional.absent();
            }
        }

        @Override
        public void put(String key, CachedResponse response) {
            try {
                _delegate.put(key, response);
            } catch (Exception ex) {
                LOG.warn("Response cache store put operation failed: key={}, response={}", key, response, ex);
            }
        }

        @Override
        public void invalidate(String key) {
            try {
                _delegate.invalidate(key);
            } catch (Exception ex) {
                LOG.warn("Response cache store invalidation operation failed: key={}", key, ex);
            }
        }
    }
}
