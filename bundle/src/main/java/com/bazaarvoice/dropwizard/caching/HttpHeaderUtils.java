package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class HttpHeaderUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpHeaderUtils.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat
            .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
            .withZoneUTC();

    private static final Function<String, CacheControl> CACHE_CONTROL_TRANSFORM = new Function<String, CacheControl>() {
        public CacheControl apply(String input) {
            return CacheControl.valueOf(input);
        }
    };

    private static final Function<String, DateTime> DATE_TIME_TRANSFORM = new Function<String, DateTime>() {
        public DateTime apply(String input) {
            return DATE_FORMAT.parseDateTime(input);
        }
    };

    /**
     * Get all the values for the given header name and concatenate with commas.
     *
     * @param headers headers to get the value from
     * @param name    name of the header to get
     * @return header value or absent if the header has not been set
     */
    public static Optional<String> getFullHeader(MultivaluedMap<String, ?> headers, String name) {
        List<?> values = headers.get(name);

        return values == null || values.size() == 0
                ? Optional.<String>absent()
                : Optional.of(Joiner.on(',').join(values));
    }

    /**
     * Get the last value for a header.
     * <p/>
     * If the header has multiple values, this will return the last one.
     * <p/>
     * For example, this would result in "deflate" being returned:
     * <code>
     * Content-Encoding: gzip
     * Content-Encoding: deflate
     * </code>
     * <p/>
     * However, this would result in "gzip, deflate" being returned:
     * <code>
     * Content-Encoding: gzip, deflate
     * </code>
     *
     * @param headers headers to get the value from
     * @param name    name of the header to get
     * @return header value or absent if the header has not been set
     */
    public static Optional<String> getLastHeader(MultivaluedMap<String, ?> headers, String name) {
        List<?> values = headers.get(name);

        return values == null || values.size() == 0
                ? Optional.<String>absent()
                : Optional.of(Objects.toString(values.get(values.size() - 1), ""));
    }

    /**
     * Get an RFC 1123 date header (using {@link #getLastHeader}).
     * <p/>
     * For example: {@link HttpHeaders#DATE}, {@link HttpHeaders#EXPIRES}
     *
     * @param headers headers to get the date from
     * @param name    header name to get the value of
     * @return date value or absent if the header is not set or the header value could not be parsed
     */
    public static Optional<DateTime> getDateHeader(MultivaluedMap<String, ?> headers, String name) {
        try {
            return getLastHeader(headers, name)
                    .transform(DATE_TIME_TRANSFORM);
        } catch (IllegalArgumentException ex) {
            // Treat invalid timestamps as absent
            return Optional.absent();
        }
    }

    public static void setDateHeader(MultivaluedMap<String, Object> headers, String name, DateTime value) {
        headers.putSingle(name, DATE_FORMAT.print(value));
    }

    /**
     * Get the {@link HttpHeaders#CACHE_CONTROL} header.
     *
     * @param headers headers to get cache-control info from
     * @return cache-control header value, if set, absent if not set or the cache-control header value is invalid
     */
    public static Optional<CacheControl> getCacheControl(MultivaluedMap<String, ?> headers) {
        try {
            return getFullHeader(headers, HttpHeaders.CACHE_CONTROL)
                    .transform(CACHE_CONTROL_TRANSFORM);
        } catch (IllegalArgumentException ex) {
            LOG.debug("Failed to parse cache-control header: {}", HttpHeaderUtils.getFullHeader(headers, HttpHeaders.CACHE_CONTROL), ex);
            return Optional.absent();
        }
    }

    /**
     * Convert the difference between two times to seconds to use as the value for the "Age" HTTP header.
     *
     * @param start the start instant
     * @param end   the end instant
     * @return age value (seconds)
     */
    public static String toAge(DateTime start, DateTime end) {
        return Integer.toString(Math.max(Seconds.secondsBetween(start, end).getSeconds(), 0));
    }

    /**
     * Generate an immutable, case-insensitive set of HTTP header names.
     *
     * @param names header names to include in the set
     * @return immutable, case-insensitive set that contains the provided names
     */
    public static Set<String> headerNames(String... names) {
        // Only the keys of the map are used. The value of the map is not used.
        StringKeyIgnoreCaseMultivaluedMap<Object> headers = new StringKeyIgnoreCaseMultivaluedMap<>();

        for (String name : names) {
            headers.add(name, true);
        }

        return Collections.unmodifiableSet(headers.keySet());
    }
}
