package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCEPT_CHARSET;
import static com.google.common.net.HttpHeaders.ACCEPT_ENCODING;
import static com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.PRAGMA;

/**
 * Cache related context information for a request.
 */
public class CacheRequestContext {
    private static final List<String> KEY_HEADERS = newArrayList(ACCEPT, ACCEPT_ENCODING, ACCEPT_LANGUAGE, ACCEPT_CHARSET);
    private static final Logger LOG = LoggerFactory.getLogger(CacheRequestContext.class);

    private final HttpRequestContext _httpContext;
    private final String _requestHash;

    private transient RequestCacheControl _cacheControl;
    private transient Boolean _pragmaNoCache;

    public CacheRequestContext(HttpRequestContext httpContext, String requestHash) {
        _httpContext = checkNotNull(httpContext);
        _requestHash = checkNotNull(requestHash);
    }

    public static CacheRequestContext build(ContainerRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            for (String header : KEY_HEADERS) {
                digest.update(header.getBytes(Charsets.UTF_8));
                digest.update((byte) 0xFD);

                List<String> headerValues = request.getRequestHeader(header);

                if (headerValues != null && headerValues.size() > 0) {
                    for (String value : headerValues) {
                        digest.update(value.getBytes(Charsets.UTF_8));
                        digest.update((byte) 0xFE);
                    }
                }

                digest.update((byte) 0xFF);
            }

            digest.update("Body".getBytes(Charsets.UTF_8));
            digest.update((byte) 0xFD);

            byte[] requestBody = request.getEntity(byte[].class);
            if (requestBody == null) {
                requestBody = new byte[0];
            }

            digest.update(requestBody);
            digest.update((byte) 0xFF);

            request.setEntityInputStream(new ByteArrayInputStream(requestBody));

            String hash = new String(Base64.encode(digest.digest()), Charsets.US_ASCII);
            return new CacheRequestContext(request, hash);
        } catch (NoSuchAlgorithmException ex) {
            // This error should not occur since SHA-1 must be included with every java distribution
            throw Throwables.propagate(ex);
        }
    }

    public HttpRequestContext getHttpContext() {
        return _httpContext;
    }

    public String getRequestHash() {
        return _requestHash;
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
