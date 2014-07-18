package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;
import com.sun.jersey.spi.container.ContainerResponse;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

class HttpHeaderUtils {
    private static final Joiner HEADER_VALUE_JOINER = Joiner.on(", ").skipNulls();

    private static final Function<Object, String> HEADER_VALUE_FORMATTER = new Function<Object, String>() {
        public String apply(@Nullable Object input) {
            return input == null
                    ? null
                    : ContainerResponse.getHeaderValue(input);
        }
    };

    private static final DateTimeFormatter RFC_1123 = DateTimeFormat
            .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
            .withZoneUTC();

    /**
     * Convert an instant to an RFC 1123 timestamp. Example: Tue, 15 Nov 1994 08:12:31 GMT
     */
    public static String dateToString(DateTime value) {
        checkNotNull(value);
        return RFC_1123.print(value);
    }

    /**
     * Parse an RFC 1123 timestamp. Example: Tue, 15 Nov 1994 08:12:31 GMT
     *
     * @throws IllegalArgumentException if the text to parse is invalid
     */
    public static DateTime parseDate(String value) {
        checkNotNull(value);
        return RFC_1123.parseDateTime(value);
    }

    /**
     * Join multiple header values with a comma. Null values are ignored. The values are transformed to string
     * with {@link ContainerResponse#getHeaderValue(Object)}.
     */
    public static String transformAndJoin(Iterable<Object> headerValues) {
        return join(Iterables.transform(headerValues, HEADER_VALUE_FORMATTER));
    }

    /**
     * Join multiple header values with a comma. Null values are ignored.
     */
    public static String join(Iterable<String> headerValues) {
        checkNotNull(headerValues);
        return HEADER_VALUE_JOINER.join(headerValues);
    }

    /**
     * Convert the difference between two times to seconds to use as the value for the "Age" HTTP header (end - start)
     * or null if end &lt; start.
     */
    public static String toAge(DateTime start, DateTime end) {
        checkNotNull(start);
        checkNotNull(end);
        int value = Seconds.secondsBetween(start, end).getSeconds();
        return value < 0
                ? null
                : Integer.toString(value);
    }

    /**
     * Generate an immutable, case-insensitive set of HTTP header names.
     */
    public static Set<String> headerNames(String... names) {
        // Only the keys of the map are used. The value of the map is not used.
        StringKeyIgnoreCaseMultivaluedMap<Object> headers = new StringKeyIgnoreCaseMultivaluedMap<Object>();

        for (String name : names) {
            headers.add(name, true);
        }

        return Collections.unmodifiableSet(headers.keySet());
    }
}
