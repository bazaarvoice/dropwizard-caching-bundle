package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.core.HttpResponseContext;

import static com.google.common.base.Preconditions.checkNotNull;

public class ResponseCache {
    private final Cache<String, Optional<CachedResponse>> _localCache;
    private final ResponseStore _store;

    public ResponseCache(Cache<String, Optional<CachedResponse>> localCache, Optional<ResponseStore> store) {
        _localCache = checkNotNull(localCache);
        _store = checkNotNull(store).orNull();
    }

    public Optional<CachedResponse> get(HttpRequestContext request) {
        String key = buildKey(request);
        Optional<CachedResponse> response = _localCache.getIfPresent(key);

        return response == null
                ? Optional.<CachedResponse>absent()
                : response;
    }

    public void put(HttpRequestContext request, HttpResponseContext response, byte[] content) {
        String key = buildKey(request);
        CachedResponse cachedResponse = new CachedResponse(response.getStatus(), response.getHttpHeaders().entrySet(), content);

        _localCache.put(key, Optional.of(cachedResponse));

        if (_store != null) {
            _store.put(key, cachedResponse);
        }
    }

    private static String buildKey(HttpRequestContext request) {
        return request.getPath();
    }
}
