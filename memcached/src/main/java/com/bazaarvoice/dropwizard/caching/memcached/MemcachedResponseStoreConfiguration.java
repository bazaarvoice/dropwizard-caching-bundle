package com.bazaarvoice.dropwizard.caching.memcached;

import com.bazaarvoice.dropwizard.caching.CachedResponse;
import com.bazaarvoice.dropwizard.caching.ResponseStore;
import com.bazaarvoice.dropwizard.caching.ResponseStoreConfiguration;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import net.spy.memcached.MemcachedClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Configuration options for storing HTTP responses in memcached.
 */
@JsonTypeName("memcached")
public class MemcachedResponseStoreConfiguration extends ResponseStoreConfiguration {
    private List<InetSocketAddress> _servers;

    public List<InetSocketAddress> getServers() {
        if (_servers == null) {
            _servers = newArrayList();
        }

        return _servers;
    }

    @JsonProperty
    public MemcachedResponseStoreConfiguration servers(List<InetSocketAddress> servers) {
        checkNotNull(servers);
        checkArgument(servers.size() > 0, "at least one server address is required");
        _servers = newArrayList(servers);
        return this;
    }

    public MemcachedResponseStoreConfiguration servers(InetSocketAddress... servers) {
        checkNotNull(servers);
        return servers(newArrayList(servers));
    }

    @Override
    public ResponseStore createStore() {
        checkState(_servers != null && _servers.size() > 0, "at least one server address is required");

        try {
            return new MemcachedResponseStore(new MemcachedClient(_servers));
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }
}
