package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.cache.LoadingCache;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.core.HttpResponseContext;

import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

public class ResponseCache {
    private final LoadingCache<String, Optional<CachedResponse>> _localCache;
    private final ResponseStore _store;

    public ResponseCache(LoadingCache<String, Optional<CachedResponse>> localCache, Optional<ResponseStore> store) {
        _localCache = checkNotNull(localCache);
        _store = checkNotNull(store).orNull();
    }

    public Optional<CachedResponse> get(HttpRequestContext request) {
        try {
            String key = buildKey(request);
            Optional<CachedResponse> response = _localCache.get(key);

            return response == null
                    ? Optional.<CachedResponse>absent()
                    : response;
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex);
        }
    }

    public void put(HttpRequestContext request, HttpResponseContext response, byte[] content) {
        String key = buildKey(request);
        CachedResponse cachedResponse = CachedResponse.build(response.getStatus(), response.getHttpHeaders(), content);

        _localCache.put(key, Optional.of(cachedResponse));

        if (_store != null) {
            _store.put(key, cachedResponse);
        }
    }

    private static String buildKey(HttpRequestContext request) {
        return request.getPath();
    }
}
