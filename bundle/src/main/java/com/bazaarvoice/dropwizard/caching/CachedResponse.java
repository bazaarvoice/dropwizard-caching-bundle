package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;
import com.sun.jersey.core.util.UnmodifiableMultivaluedMap;
import com.sun.jersey.spi.container.ContainerResponse;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Response loaded from the cache.
 */
public class CachedResponse {
    /**
     * Names of HTTP headers that are automatically excluded from the cached response headers. The set implementation is
     * case-insensitive.
     * <p/>
     * Age is non-cacheable because it must be recalculated each time the response is returned.
     */
    public static final Set<String> NON_CACHEABLE_HEADERS = HttpHeaderUtils.headerNames("Age");

    private transient DateTime _date;
    private transient Optional<CacheControl> _cacheControl;
    private transient Optional<Duration> _maxAge;
    private transient Optional<DateTime> _expires;

    private final int _statusCode;
    private final MultivaluedMap<String, String> _responseHeaders;
    private final byte[] _responseContent;

    public CachedResponse(int statusCode, MultivaluedMap<String, String> headers, byte[] content) {
        _statusCode = statusCode;
        _responseHeaders = checkNotNull(headers);
        _responseContent = checkNotNull(content);
    }

    public static CachedResponse build(int statusCode, MultivaluedMap<String, Object> headers, byte[] content) {
        checkNotNull(headers);
        return new CachedResponse(statusCode, copyHeaders(headers.entrySet()), content);
    }

    private static MultivaluedMap<String, String> copyHeaders(Iterable<Map.Entry<String, List<Object>>> headers) {
        StringKeyIgnoreCaseMultivaluedMap<String> copy = new StringKeyIgnoreCaseMultivaluedMap<>();

        for (Map.Entry<String, List<Object>> header : headers) {
            if (!NON_CACHEABLE_HEADERS.contains(header.getKey())) {
                for (Object headerValue : header.getValue()) {
                    copy.add(header.getKey(), ContainerResponse.getHeaderValue(headerValue));
                }
            }
        }

        return new UnmodifiableMultivaluedMap<>(copy);
    }

    /**
     * Get the date the response was generated.
     * <p/>
     * Retrieves the {@link HttpHeaders#DATE} header or current time if no date header is found.
     *
     * @return response date
     */
    public DateTime getDate() {
        if (_date == null) {
            _date = HttpHeaderUtils
                    .getDateHeader(_responseHeaders, HttpHeaders.DATE)
                    .or(DateTime.now());
        }

        return _date;
    }

    /**
     * Get the {@link HttpHeaders#CACHE_CONTROL} header, if set.
     *
     * @return cache-control header or absent if cache-control header is not set
     */
    public Optional<CacheControl> getCacheControl() {
        if (_cacheControl == null) {
            _cacheControl = HttpHeaderUtils.getCacheControl(_responseHeaders);
        }

        return _cacheControl;
    }

    public Optional<Duration> getMaxAge() {
        if (_maxAge == null) {
            Optional<CacheControl> cacheControl = getCacheControl();

            if (cacheControl.isPresent()) {
                int maxAgeSecs = CacheControlUtils.getSharedCacheMaxAge(cacheControl.get());

                _maxAge = maxAgeSecs < 0
                        ? Optional.<Duration>absent()
                        : Optional.of(Duration.standardSeconds(maxAgeSecs));
            } else {
                _maxAge = Optional.absent();
            }
        }

        return _maxAge;
    }

    public Optional<DateTime> getExpires() {
        if (_expires == null) {
            Optional<Duration> maxAge = getMaxAge();

            _expires = maxAge.isPresent()
                    ? Optional.of(getDate().plus(maxAge.get()))
                    : HttpHeaderUtils.getDateHeader(_responseHeaders, HttpHeaders.EXPIRES);
        }

        return _expires;
    }

    public boolean isCacheable() {
        Optional<CacheControl> cacheControl = getCacheControl();
        return cacheControl.isPresent() && !CacheControlUtils.isResponseCacheable(cacheControl.get());
    }

    /**
     * Test if this response has expired.
     * <p/>
     * Tests if {@link #getExpires()} returns a value and, if so, the expiration time is before the provided instant.
     *
     * @param at expiration instant to test
     * @return true if the response has expired and should not be used
     */
    public boolean isExpired(DateTime at) {
        Optional<DateTime> expires = getExpires();
        return expires.isPresent() && expires.get().isBefore(at);
    }

    public byte[] getResponseContent() {
        return _responseContent;
    }

    /**
     * Immutable map from response header name (case insensitive) to list of header values.
     *
     * @return response headers
     */
    public MultivaluedMap<String, String> getResponseHeaders() {
        return _responseHeaders;
    }

    public int getStatusCode() {
        return _statusCode;
    }
}
