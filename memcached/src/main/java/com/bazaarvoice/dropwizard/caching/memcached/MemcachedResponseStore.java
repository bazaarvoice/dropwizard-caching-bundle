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

    public MemcachedResponseStore(MemcachedClient client) {
        _client = checkNotNull(client);
    }

    @Override
    public Optional<CachedResponse> get(String key) {
        try {
            return Optional.fromNullable(_client.get(key, CachedResponseTranscoder.INSTANCE));
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
        DateTime expires = response.getExpires().orNull();

        if (expires != null) {
            try {
                _client.set(key, (int) (expires.getMillis() / 1000), response, CachedResponseTranscoder.INSTANCE);
            } catch (IllegalStateException ex) {
                LOG.warn("Memcached store operation failed. Internal error: key={}", key, ex);
            } catch (Throwable ex) {
                LOG.warn("Memcached store operation failed: key={}", key, ex);
            }
        }
    }
}
