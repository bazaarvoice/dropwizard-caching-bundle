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
import com.sun.jersey.api.core.HttpResponseContext;
import com.sun.jersey.spi.container.ContainerResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.CacheControl;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.AGE;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.DATE;
import static com.google.common.net.HttpHeaders.EXPIRES;

/**
 * Cache related context information for a response.
 */
public class CacheResponseContext {
    private static final Logger LOG = LoggerFactory.getLogger(CacheResponseContext.class);

    private final HttpResponseContext _httpContext;

    private transient CacheControl _cacheControl;
    private transient Optional<DateTime> _date;

    public CacheResponseContext(HttpResponseContext httpContext) {
        _httpContext = checkNotNull(httpContext);
    }

    public HttpResponseContext getHttpContext() {
        return _httpContext;
    }

    /**
     * Get the HTTP response status code (e.g. 200, 404, 503, etc).
     *
     * @return status code
     */
    public int getStatusCode() {
        return _httpContext.getStatus();
    }

    /**
     * Get the cache control options specified in the response.
     * <p/>
     * Although {@link CacheControl} is mutable, the returned value should be treated as immutable.
     *
     * @return response cache control options
     */
    public CacheControl getCacheControl() {
        if (_cacheControl == null) {
            List<Object> headerValues = _httpContext.getHttpHeaders().get(CACHE_CONTROL);

            if (headerValues != null) {
                try {
                    _cacheControl = CacheControl.valueOf(HttpHeaderUtils.transformAndJoin(headerValues));
                } catch (Exception ex) {
                    LOG.debug("Failed to parse cache-control header: value='{}'", headerValues, ex);
                }
            }

            if (_cacheControl == null) {
                _cacheControl = new CacheControl();
            }
        }

        return _cacheControl;
    }

    /**
     * Get the max time, in seconds, the response should be served from a shared cache or -1 if no max age has been set.
     *
     * @return response shared cache max age, in seconds, or -1
     */
    public int getSharedCacheMaxAge() {
        return CacheControlUtils.getSharedCacheMaxAge(getCacheControl());
    }

    /**
     * Get the {@link com.google.common.net.HttpHeaders#DATE} HTTP header.
     *
     * @return date header value or absent if the header is not set or the value is invalid
     */
    public Optional<DateTime> getDate() {
        if (_date == null) {
            Object headerValue = _httpContext.getHttpHeaders().getFirst(DATE);

            _date = Optional.absent();

            if (headerValue != null) {
                try {
                    _date = Optional.of(HttpHeaderUtils.parseDate(ContainerResponse.getHeaderValue(headerValue)));
                } catch (Exception ex) {
                    LOG.debug("Error parsing date header: value={}", headerValue, ex);
                }
            }
        }

        return _date;
    }

    /**
     * Set the {@link com.google.common.net.HttpHeaders#DATE} HTTP header.
     *
     * @param date instant to set the date header to
     */
    public void setDate(DateTime date) {
        _date = Optional.of(date);
        _httpContext.getHttpHeaders().putSingle(DATE, HttpHeaderUtils.dateToString(date));
    }

    /**
     * Set the {@link com.google.common.net.HttpHeaders#EXPIRES} HTTP header.
     *
     * @param date instant to set the expires header to
     */
    public void setExpires(DateTime date) {
        checkNotNull(date);
        _httpContext.getHttpHeaders().putSingle(EXPIRES, HttpHeaderUtils.dateToString(date));
    }

    /**
     * Set the {@link com.google.common.net.HttpHeaders#AGE} HTTP header as the difference between two instants
     * (end - start).
     * <p/>
     * Sets the age to 0 if end &lt; start.
     *
     * @param start starting instant
     * @param end   ending instant
     */
    public void setAge(DateTime start, DateTime end) {
        _httpContext.getHttpHeaders().putSingle(AGE, HttpHeaderUtils.toAge(start, end));
    }

    /**
     * Set the {@link com.google.common.net.HttpHeaders#AGE} HTTP header.
     *
     * @param seconds number of seconds to set the header to (>= 0)
     */
    public void setAge(int seconds) {
        checkArgument(seconds >= 0, "seconds must be >= 0");
        _httpContext.getHttpHeaders().putSingle(AGE, Integer.toString(seconds));
    }
}
