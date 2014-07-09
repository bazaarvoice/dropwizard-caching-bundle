package com.bazaarvoice.dropwizard.caching;

import com.sun.jersey.api.core.HttpRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.PRAGMA;

/**
 * Cache related context information for a request.
 */
public class CacheRequestContext {
    private static final Logger LOG = LoggerFactory.getLogger(CacheRequestContext.class);

    private final HttpRequestContext _httpContext;

    private transient RequestCacheControl _cacheControl;
    private transient Boolean _pragmaNoCache;

    public CacheRequestContext(HttpRequestContext httpContext) {
        _httpContext = checkNotNull(httpContext);
    }

    public HttpRequestContext getHttpContext() {
        return _httpContext;
    }

    /**
     * Get the cache control options set for the request.
     * <p/>
     * Returns {@link RequestCacheControl#DEFAULT} if no cache-control header is present or the cache-control header is
     * invalid.
     *
     * @return request cache control options
     */
    public RequestCacheControl getCacheControl() {
        if (_cacheControl == null) {
            String cacheControlHeader = _httpContext.getHeaderValue(CACHE_CONTROL);

            try {
                _cacheControl = isNullOrEmpty(cacheControlHeader)
                        ? RequestCacheControl.DEFAULT
                        : RequestCacheControl.valueOf(cacheControlHeader);
            } catch (IllegalArgumentException ex) {
                _cacheControl = RequestCacheControl.DEFAULT;
                LOG.debug("Invalid request cache control header", ex);
            }
        }

        return _cacheControl;
    }

    /**
     * True if the <code>Pragma: no-cache</code> header is set on the request.
     * <p/>
     * This header should be treated as equivalent to <code>Cache-Control: no-cache</code>.
     *
     * @return true if the pragma no-cache header is set
     */
    public boolean isPragmaNoCache() {
        if (_pragmaNoCache == null) {
            List<String> values = _httpContext.getRequestHeader(PRAGMA);

            _pragmaNoCache = false;

            if (values != null) {
                for (String value : values) {
                    if ("no-cache".equalsIgnoreCase(value)) {
                        _pragmaNoCache = true;
                    }
                }
            }
        }

        return _pragmaNoCache;
    }
}
