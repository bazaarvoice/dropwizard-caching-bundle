package com.bazaarvoice.dropwizard.caching.memcached;

import com.bazaarvoice.dropwizard.caching.CachedResponse;
import com.bazaarvoice.dropwizard.caching.ResponseStore;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import net.spy.memcached.MemcachedClient;
import org.joda.time.DateTime;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HTTP response cache store backed by memcached.
 */
public class MemcachedResponseStore extends ResponseStore {
    private static final char KEY_PREFIX_SEPARATOR = ':';

    private final MemcachedClient _client;
    private final String _keyPrefix;
    private final boolean _readOnly;

    public MemcachedResponseStore(MemcachedClient client, String keyPrefix, boolean readOnly) {
        checkNotNull(keyPrefix);

        _client = checkNotNull(client);
        _readOnly = readOnly;

        keyPrefix = encodeAscii(keyPrefix);

        if (keyPrefix.length() > 0 && keyPrefix.charAt(keyPrefix.length() - 1) == KEY_PREFIX_SEPARATOR) {
            _keyPrefix = keyPrefix.substring(0, keyPrefix.lastIndexOf(KEY_PREFIX_SEPARATOR));
        } else {
            _keyPrefix = keyPrefix;
        }
    }

    @Override
    public Optional<CachedResponse> get(String key) {
        checkNotNull(key);
        return Optional.fromNullable(_client.get(buildKey(key), CachedResponseTranscoder.INSTANCE));
    }

    @Override
    public void put(String key, CachedResponse response) {
        checkNotNull(key);
        checkNotNull(response);

        if (!_readOnly) {
            DateTime expires = response.getExpires().orNull();

            if (expires != null) {
                _client.set(buildKey(key), (int) (expires.getMillis() / 1000), response, CachedResponseTranscoder.INSTANCE);
            }
        }
    }

    @Override
    public void invalidate(String key) {
        checkNotNull(key);

        if (!_readOnly) {
            _client.delete(buildKey(key));
        }
    }

    private String buildKey(String key) {
        String result = encodeAscii(key);

        if (_keyPrefix.length() > 0) {
            StringBuilder buffer = new StringBuilder(_keyPrefix.length() + 1 + key.length());
            buffer.append(_keyPrefix);

            if (key.charAt(0) != KEY_PREFIX_SEPARATOR) {
                buffer.append(KEY_PREFIX_SEPARATOR);
            }

            buffer.append(key);
            result = buffer.toString();
        }

         return result;
    }

    private String encodeAscii(String value) {
        try {
            return URLEncoder.encode(value, "us-ascii");
        } catch (UnsupportedEncodingException ex) {
            // US ASCII should be supported everywhere, so this exception should not occur
            throw Throwables.propagate(ex);
        }
    }
}
