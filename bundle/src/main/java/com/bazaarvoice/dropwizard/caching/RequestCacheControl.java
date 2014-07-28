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
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.core.header.reader.HttpHeaderReader;

import java.text.ParseException;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Cache-Control header options that are valid for an HTTP request.
 * <p/>
 * For more info, see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9">RFC 2616, Section 14.9</a>.
 * Also, <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.4">RFC 2616, Section 14.9.4</a> applies
 * to request cache control options.
 */
public class RequestCacheControl {
    public static final RequestCacheControl DEFAULT = new RequestCacheControl();

    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    private boolean _noCache;
    private boolean _noStore;
    private int _maxAge = -1;
    private int _maxStale = -1;
    private int _minFresh = -1;
    private boolean _noTransform;
    private boolean _onlyIfCached;
    private Map<String, Optional<String>> _cacheExtension;

    /**
     * Creates a new instance of public class RequestCacheControl by parsing the supplied string.
     *
     * @param value the cache control string
     * @return the newly created RequestCacheControl
     * @throws IllegalArgumentException if the supplied string cannot be parsed
     */
    public static RequestCacheControl valueOf(String value) {
        checkNotNull(value);

        try {
            HttpHeaderReader reader = HttpHeaderReader.newInstance(value);
            RequestCacheControl cacheControl = new RequestCacheControl();
            ImmutableMap.Builder<String, Optional<String>> cacheExtension = ImmutableMap.builder();

            while (reader.hasNext()) {
                String directive = reader.nextToken();

                if ("no-cache".equalsIgnoreCase(directive)) {
                    cacheControl._noCache = true;
                } else if ("no-store".equalsIgnoreCase(directive)) {
                    cacheControl._noStore = true;
                } else if ("max-stale".equalsIgnoreCase(directive)) {
                    cacheControl._maxStale = reader.hasNextSeparator('=', false)
                            ? readDeltaSeconds(reader, directive)
                            : Integer.MAX_VALUE;
                } else if ("max-age".equalsIgnoreCase(directive)) {
                    cacheControl._maxAge = readDeltaSeconds(reader, directive);
                } else if ("min-fresh".equalsIgnoreCase(directive)) {
                    cacheControl._minFresh = readDeltaSeconds(reader, directive);
                } else if ("no-transform".equalsIgnoreCase(directive)) {
                    cacheControl._noTransform = true;
                } else if ("only-if-cached".equalsIgnoreCase(directive)) {
                    cacheControl._onlyIfCached = true;
                } else {
                    String directiveValue = null;

                    if (reader.hasNextSeparator('=', false)) {
                        reader.nextSeparator('=');
                        directiveValue = reader.nextTokenOrQuotedString();
                    }

                    cacheExtension.put(directive.toLowerCase(), Optional.fromNullable(directiveValue));
                }

                if (reader.hasNextSeparator(',', true)) {
                    reader.nextSeparator(',');
                }
            }

            cacheControl._cacheExtension = cacheExtension.build();
            return cacheControl;
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Error parsing request cache control: value='" + value + "'", ex);
        }
    }

    /**
     * If true, the server MUST NOT use a cached copy when responding to the request.
     *
     * @return true to force a fresh request
     */
    public boolean isNoCache() {
        return _noCache;
    }

    /**
     * If true, a cache MUST NOT store any part of either this request or any response to it.
     *
     * @return true to force the response to not be cached
     */
    public boolean isNoStore() {
        return _noStore;
    }

    /**
     * Indicates that the client is willing to accept a response whose age is no greater than the specified time in
     * seconds.
     * <p/>
     * Unless {@link #getMaxStale()} directive is also included, the client is not willing to accept a stale response.
     *
     * @return max age, in seconds, or -1 if the max-age option was not set
     */
    public int getMaxAge() {
        return _maxAge;
    }

    /**
     * Indicates that the client is willing to accept a response that has exceeded its expiration time.
     * <p/>
     * If max-stale is assigned a value, then the client is willing to accept a response that has exceeded its
     * expiration time by no more than the specified number of seconds. If {@link Integer#MAX_VALUE} is assigned to
     * max-stale, then the client is willing to accept a stale response of any age.
     *
     * @return max response staleness, in seconds, or -1 if the max-stale option was not set
     */
    public int getMaxStale() {
        return _maxStale;
    }

    /**
     * Indicates that the client is willing to accept a response whose freshness lifetime is no less than its current
     * age plus the specified time in seconds. That is, the client wants a response that will still be fresh for at
     * least the specified number of seconds.
     *
     * @return min freshness, in seconds, or -1 if the min-fresh option was not set
     */
    public int getMinFresh() {
        return _minFresh;
    }

    /**
     * If true, an intermediate cache or proxy MUST NOT change those headers that are listed in
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec13.5.2">section 13.5.2</a> as being subject to
     * the no-transform directive. This implies that the cache or proxy MUST NOT change any aspect of the entity-body
     * that is specified by these headers, including the value of the entity-body itself.
     *
     * @return true if the cache/proxy must not modify the response
     */
    public boolean isNoTransform() {
        return _noTransform;
    }

    /**
     * If true, a cache SHOULD either respond using a cached entry that is consistent with the other constraints of the
     * request, or respond with a 504 (Gateway Timeout) status.
     * <p/>
     * If a group of caches is being operated as a unified system with good internal connectivity, such a request MAY be
     * forwarded within that group of caches.
     *
     * @return true to only serve the response from a cache
     */
    public boolean isOnlyIfCached() {
        return _onlyIfCached;
    }

    /**
     * Unknown or unsupported cache control directives.
     * <p/>
     * If the directive appeared as a bare value, the value in the map will be absent. The keys are lowercase.
     * These directives can be ignored by an intermediate cache/proxy, but they must be passed to any upstream caches.
     *
     * @return immutable map from directive name to value
     */
    public Map<String, Optional<String>> getCacheExtension() {
        if (_cacheExtension == null) {
            _cacheExtension = ImmutableMap.of();
        }

        return _cacheExtension;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (_noCache ? 1 : 0);
        hash = 41 * hash + (_noStore ? 1 : 0);
        hash = 41 * hash + _maxAge;
        hash = 41 * hash + _maxStale;
        hash = 41 * hash + _minFresh;
        hash = 41 * hash + (_noTransform ? 1 : 0);
        hash = 41 * hash + (_onlyIfCached ? 1 : 0);
        hash = 41 * hash + (_cacheExtension != null ? _cacheExtension.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RequestCacheControl)) {
            return false;
        }

        RequestCacheControl other = (RequestCacheControl) obj;
        return _noCache == other._noCache &&
                _noStore == other._noStore &&
                _maxAge == other._maxAge &&
                _maxStale == other._maxStale &&
                _minFresh == other._minFresh &&
                _noTransform == other._noTransform &&
                _onlyIfCached == other._onlyIfCached &&
                (
                        // Empty and null are equivalent
                        _cacheExtension == other._cacheExtension ||
                                (_cacheExtension == null && other._cacheExtension.size() == 0) ||
                                (other._cacheExtension == null && _cacheExtension.size() == 0) ||
                                _cacheExtension.equals(other._cacheExtension)
                );
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        if (_noCache) {
            appendDirective(buffer, "no-cache");
        }

        if (_noStore) {
            appendDirective(buffer, "no-store");
        }

        if (_maxAge >= 0) {
            appendDirective(buffer, "max-age", _maxAge);
        }

        if (_maxStale == Integer.MAX_VALUE) {
            appendDirective(buffer, "max-stale");
        } else if (_maxStale >= 0) {
            appendDirective(buffer, "max-stale", _maxStale);
        }

        if (_minFresh >= 0) {
            appendDirective(buffer, "min-fresh", _minFresh);
        }

        if (_noTransform) {
            appendDirective(buffer, "no-transform");
        }

        if (_onlyIfCached) {
            appendDirective(buffer, "only-if-cached");
        }

        if (_cacheExtension != null) {
            for (Map.Entry<String, Optional<String>> entry : _cacheExtension.entrySet()) {
                appendDirective(buffer, entry.getKey(), entry.getValue());
            }
        }

        return buffer.toString();
    }

    private static void appendDirective(StringBuilder buffer, String key) {
        if (buffer.length() > 0) {
            buffer.append(", ");
        }

        buffer.append(key);
    }

    private static void appendDirective(StringBuilder buffer, String key, int value) {
        appendDirective(buffer, key);
        buffer.append('=').append(value);
    }

    private static void appendDirective(StringBuilder buffer, String key, Optional<String> value) {
        appendDirective(buffer, key);

        if (value.isPresent()) {
            buffer.append('=').append(quoteDirective(value.get()));
        }
    }

    private static String quoteDirective(String value) {
        return WHITESPACE.matcher(value).find()
                ? '"' + value + '"'
                : value;
    }

    private static int readDeltaSeconds(HttpHeaderReader reader, String directiveName)
            throws ParseException {
        reader.nextSeparator('=');
        int index = reader.getIndex();
        int value;

        try {
            value = Integer.parseInt(reader.nextToken());
        } catch (NumberFormatException nfe) {
            ParseException pe = new ParseException("Error parsing integer value for " + directiveName + " directive", index);
            pe.initCause(nfe);
            throw pe;
        }

        if (value < 0) {
            throw new ParseException("Value for " + directiveName + " directive is negative", index);
        }

        return value;
    }
}
