package com.bazaarvoice.dropwizard.caching.memcached;

import com.bazaarvoice.dropwizard.caching.CachedResponse;
import com.bazaarvoice.dropwizard.caching.ResponseStore;
import com.google.common.base.Optional;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.OperationTimeoutException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HTTP response cache store backed by memcached.
 */
public class MemcachedResponseStore extends ResponseStore {
    private static final Logger LOG = LoggerFactory.getLogger(MemcachedResponseStore.class);

    private final MemcachedClient _client;
    private final String _keyPrefix;
    private final boolean _readOnly;

    public MemcachedResponseStore(MemcachedClient client, String keyPrefix, boolean readOnly) {
        checkNotNull(keyPrefix);

        _client = checkNotNull(client);
        _readOnly = readOnly;

        if (keyPrefix.endsWith("/")) {
            _keyPrefix = keyPrefix.substring(0, keyPrefix.lastIndexOf('/'));
        } else {
            _keyPrefix = keyPrefix;
        }
    }

    @Override
    public Optional<CachedResponse> get(String key) {
        checkNotNull(key);

        try {
            return Optional.fromNullable(_client.get(buildKey(key), CachedResponseTranscoder.INSTANCE));
        } catch (OperationTimeoutException ex) {
            LOG.warn("Memcached get operation timed out: key={}", key, ex);
            return Optional.absent();
        } catch (IllegalStateException ex) {
            LOG.warn("Memcached get operation failed. Internal error: key={}", key, ex);
            return Optional.absent();
        } catch (Throwable ex) {
            LOG.warn("Memcached get operation failed: key={}", key, ex);
            return Optional.absent();
        }
    }

    @Override
    public void put(String key, CachedResponse response) {
        checkNotNull(key);
        checkNotNull(response);

        if (_readOnly) {
            DateTime expires = response.getExpires().orNull();

            if (expires != null) {
                try {
                    _client.set(buildKey(key), (int) (expires.getMillis() / 1000), response, CachedResponseTranscoder.INSTANCE);
                } catch (IllegalStateException ex) {
                    LOG.warn("Memcached store operation failed. Internal error: key={}", key, ex);
                } catch (Throwable ex) {
                    LOG.warn("Memcached store operation failed: key={}", key, ex);
                }
            }
        }
    }

    @Override
    public void invalidate(String key) {
        checkNotNull(key);

        if (_readOnly) {
            try {
                _client.delete(buildKey(key));
            } catch (IllegalStateException ex) {
                LOG.warn("Memcached delete operation failed. Internal error: key={}", key, ex);
            } catch (Throwable ex) {
                LOG.warn("Memcached delete operation failed: key={}", key, ex);
            }
        }
    }

    private String buildKey(String key) {
        if (_keyPrefix.length() == 0) {
            return key;
        }

        StringBuilder buffer = new StringBuilder(_keyPrefix.length() + 1 + key.length());
        buffer.append(_keyPrefix);

        if (key.charAt(0) != '/') {
            buffer.append('/');
        }

        buffer.append(key);
        return buffer.toString();
    }
}
