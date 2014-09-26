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
package com.bazaarvoice.dropwizard.caching.memcached;

import com.bazaarvoice.dropwizard.caching.CachedResponse;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.primitives.Bytes;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import javax.ws.rs.core.MultivaluedMap;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

/**
 * Transcoder that converts {@link CachedResponse} instances to/from bytes for storing in memcached.
 */
public class CachedResponseTranscoder implements Transcoder<CachedResponse> {
    private static final byte[] HEADER_SEPARATOR = new byte[]{'\r', '\n', '\r', '\n'};
    private static final Splitter STATUS_SPLITTER = Splitter.on(' ').trimResults();
    private static final Splitter HEADER_SPLITTER = Splitter.on(':').trimResults().limit(2);

    public static final CachedResponseTranscoder INSTANCE = new CachedResponseTranscoder();

    private CachedResponseTranscoder() {
        // Nothing to do
    }

    @Override
    public boolean asyncDecode(CachedData d) {
        return false;
    }

    @Override
    public CachedData encode(CachedResponse o) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(buffer, Charsets.US_ASCII);

            writer.write(format("HTTP/1.1 %d \r\n", o.getStatusCode()));

            for (Map.Entry<String, List<String>> entry : o.getResponseHeaders().entrySet()) {
                for (String value : entry.getValue()) {
                    writer.write(entry.getKey());
                    writer.write(": ");
                    writer.write(value);
                    writer.write("\r\n");
                }
            }

            writer.write("\r\n");
            writer.flush();

            buffer.write(o.getResponseContent());

            return new CachedData(0, buffer.toByteArray(), getMaxSize());
        } catch (IOException ex) {
            // There should be no IO exceptions since the operations are done with a memory stream
            throw Throwables.propagate(ex);
        }
    }

    @Override
    public CachedResponse decode(CachedData d) {
        try {
            byte[] cachedData = d.getData();
            int headerSeparatorIndex = Bytes.indexOf(cachedData, HEADER_SEPARATOR);

            if (headerSeparatorIndex < 0) {
                return null;
            }

            BufferedReader headerReader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(cachedData, 0, headerSeparatorIndex),
                    Charsets.US_ASCII)
            );

            int statusCode = readStatusCode(headerReader);
            MultivaluedMap<String, String> headers = readHeaders(headerReader);
            byte[] responseContent = Arrays.copyOfRange(cachedData, headerSeparatorIndex + HEADER_SEPARATOR.length, cachedData.length);

            return new CachedResponse(statusCode, headers, responseContent);
        } catch (IOException ex) {
            throw new RuntimeException("Corrupted cache entry", ex);
        }
    }

    @Override
    public int getMaxSize() {
        return CachedData.MAX_SIZE;
    }

    private static MultivaluedMap<String, String> readHeaders(BufferedReader reader) throws IOException {
        StringKeyIgnoreCaseMultivaluedMap<String> headers = new StringKeyIgnoreCaseMultivaluedMap<String>();
        String line;

        while ((line = reader.readLine()) != null) {
            List<String> headerParts = newArrayList(HEADER_SPLITTER.split(line));

            if (headerParts.size() != 2) {
                throw new IOException("Corrupt header");
            }

            String key = headerParts.get(0);
            String value = headerParts.get(1);

            if (isNullOrEmpty(key)) {
                throw new IOException("Corrupt header");
            }

            if (!isNullOrEmpty(value)) {
                headers.add(key, value);
            }
        }

        return headers;
    }

    private static int readStatusCode(BufferedReader reader) throws IOException {
        String statusLine = reader.readLine();

        if (statusLine == null) {
            throw new IOException("Missing status line");
        }

        List<String> parts = newArrayList(STATUS_SPLITTER.split(statusLine));

        if (parts.size() != 3 || !parts.get(0).equals("HTTP/1.1")) {
            throw new IOException("Corrupt status line");
        }

        try {
            return Integer.parseInt(parts.get(1));
        } catch (NumberFormatException ex) {
            throw new IOException("Corrupt status line", ex);
        }
    }
}
