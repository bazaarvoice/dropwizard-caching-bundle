/*
 * Copyright 2014 Bazaarvoice, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Optional;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;
import com.sun.jersey.core.util.UnmodifiableMultivaluedMap;
import com.sun.jersey.spi.container.ContainerResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.DATE;
import static com.google.common.net.HttpHeaders.EXPIRES;

/**
 * Response loaded from the cache.
 */
public class CachedResponse {
    private static final Logger LOG = LoggerFactory.getLogger(CachedResponse.class);

    /**
     * Names of HTTP headers that are automatically excluded from the cached response headers. The set implementation is
     * case-insensitive.
     * <p/>
     * Age is non-cacheable because it must be recalculated each time the response is returned.
     */
    public static final Set<String> NON_CACHEABLE_HEADERS = HttpHeaderUtils.headerNames("Age");

    private transient DateTime _date;
    private transient Optional<CacheControl> _cacheControl;
    private transient Optional<DateTime> _expires;

    private final int _statusCode;
    private final MultivaluedMap<String, String> _responseHeaders;
    private final byte[] _responseContent;

    public CachedResponse(int statusCode, MultivaluedMap<String, String> headers, byte[] content) {
        _statusCode = statusCode;
        _responseHeaders = checkNotNull(headers);
        _responseContent = checkNotNull(content);
    }

    @Override
    public int hashCode() {
        int hash = 234290234;
        hash = (31 * hash) + _statusCode;
        hash = (31 * hash) + hashCode(_responseHeaders);
        hash = (31 * hash) + Arrays.hashCode(_responseContent);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CachedResponse)) {
            return false;
        } else if (obj == this) {
            return true;
        }

        CachedResponse other = (CachedResponse) obj;
        return _statusCode == other._statusCode &&
                Arrays.equals(_responseContent, other._responseContent) &&
                equals(_responseHeaders, other._responseHeaders);
    }

    public static CachedResponse build(int statusCode, MultivaluedMap<String, Object> headers, byte[] content) {
        checkNotNull(headers);
        return new CachedResponse(statusCode, copyHeaders(headers.entrySet()), content);
    }

    public Response.ResponseBuilder response(DateTime now) {
        Response.ResponseBuilder responseBuilder = Response
                .status(getStatusCode())
                .entity(getResponseContent())
                .header("Age", HttpHeaderUtils.toAge(getDate(), now));

        for (Map.Entry<String, List<String>> header : getResponseHeaders().entrySet()) {
            for (String headerValue : header.getValue()) {
                responseBuilder.header(header.getKey(), headerValue);
            }
        }

        return responseBuilder;
    }

    private static boolean equals(MultivaluedMap<String, String> headers1, MultivaluedMap<String, String> headers2) {
        for (Map.Entry<String, List<String>> h1 : headers1.entrySet()) {
            if (!equals(h1.getValue(), headers2.get(h1.getKey()))) {
                return false;
            }
        }

        for (String key : headers2.keySet()) {
            if (!headers1.containsKey(key)) {
                return false;
            }
        }

        return true;
    }

    private static boolean equals(List<String> l1, List<String> l2) {
        if (l1 == null) {
            return l2 == null;
        } else if (l2 == null) {
            return false;
        } else if (l1.size() != l2.size()) {
            return false;
        }

        for (int i = 0; i < l1.size(); i += 1) {
            if (!Objects.equals(l1.get(i), l2.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static int hashCode(MultivaluedMap<String, String> headers) {
        int hash = 3402034;

        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            hash = (31 * hash) + header.getKey().toLowerCase().hashCode();

            for (String value : header.getValue()) {
                hash = (31 * hash) + value.hashCode();
            }
        }

        return hash;
    }

    private static MultivaluedMap<String, String> copyHeaders(Iterable<Map.Entry<String, List<Object>>> headers) {
        StringKeyIgnoreCaseMultivaluedMap<String> copy = new StringKeyIgnoreCaseMultivaluedMap<String>();

        for (Map.Entry<String, List<Object>> header : headers) {
            if (!NON_CACHEABLE_HEADERS.contains(header.getKey())) {
                for (Object headerValue : header.getValue()) {
                    copy.add(header.getKey(), ContainerResponse.getHeaderValue(headerValue));
                }
            }
        }

        return new UnmodifiableMultivaluedMap<String, String>(copy);
    }

    /**
     * True if this response has a configured expiration time.
     */
    public boolean hasExpiration() {
        return getExpires().isPresent();
    }

    /**
     * True if the response has an expiration time and that expiration is after the provided instant.
     */
    public boolean isExpired(DateTime now) {
        return hasExpiration() && getExpires().get().isAfter(now);
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
            String dateString = _responseHeaders.getFirst(DATE);

            if (dateString != null) {
                try {
                    _date = HttpHeaderUtils.parseDate(dateString);
                } catch (Exception ex) {
                    LOG.debug("Failed to parse date header: value={}", dateString, ex);
                }
            }

            if (_date == null) {
                _date = DateTime.now();
            }
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
            List<String> headerValues = _responseHeaders.get(CACHE_CONTROL);

            _cacheControl = Optional.absent();

            if (headerValues != null) {
                try {
                    _cacheControl = Optional.of(CacheControl.valueOf(HttpHeaderUtils.join(headerValues)));
                } catch (Exception ex) {
                    LOG.debug("Failed to parse cache-control header: value='{}'", headerValues, ex);
                }
            }
        }

        return _cacheControl;
    }

    public Optional<DateTime> getExpires() {
        if (_expires == null) {
            CacheControl cacheControl = getCacheControl().orNull();

            _expires = Optional.absent();

            if (cacheControl != null) {
                int maxAge = CacheControlUtils.getSharedCacheMaxAge(cacheControl);

                if (maxAge >= 0) {
                    _expires = Optional.of(getDate().plusSeconds(maxAge));
                } else {
                    String expiresString = _responseHeaders.getFirst(EXPIRES);

                    if (expiresString != null) {
                        try {
                            _expires = Optional.of(HttpHeaderUtils.parseDate(expiresString));
                        } catch (Exception ex) {
                            LOG.debug("Failed to parse expires header: value={}", expiresString, ex);
                        }
                    }
                }
            }
        }

        return _expires;
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
