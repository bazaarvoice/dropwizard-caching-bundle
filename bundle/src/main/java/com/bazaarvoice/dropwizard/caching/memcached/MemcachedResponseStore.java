package com.bazaarvoice.dropwizard.caching.memcached;

import com.bazaarvoice.dropwizard.caching.CachedResponse;
import com.bazaarvoice.dropwizard.caching.ResponseStore;
import com.google.common.base.Optional;
import net.spy.memcached.MemcachedClient;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HTTP response cache store backed by memcached.
 */
public class MemcachedResponseStore extends ResponseStore {
    private final MemcachedClient _client;
    private final String _keyPrefix;
    private final boolean _readOnly;

    public MemcachedResponseStore(MemcachedClient client, String keyPrefix, boolean readOnly) {
        _client = checkNotNull(client);
        _readOnly = readOnly;
        _keyPrefix = checkNotNull(keyPrefix);
    }

    @Override
    public Optional<CachedResponse> get(String key) {
        checkNotNull(key);
        checkArgument(key.length() > 0, "key can not be empty");
        return Optional.fromNullable(_client.get(buildKey(key), CachedResponseTranscoder.INSTANCE));
    }

    @Override
    public void put(String key, CachedResponse response) {
        checkNotNull(key);
        checkArgument(key.length() > 0, "key can not be empty");
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
        checkArgument(key.length() > 0, "key can not be empty");

        if (!_readOnly) {
            _client.delete(buildKey(key));
        }
    }

    private String buildKey(String key) {
        return KeyUtils.truncateKey(
                _keyPrefix.length() > 0
                        ? _keyPrefix + key
                        : key);
    }
}
