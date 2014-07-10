package com.bazaarvoice.dropwizard.caching.memcached;

import com.bazaarvoice.dropwizard.caching.ResponseStore;
import com.bazaarvoice.dropwizard.caching.ResponseStoreConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import net.spy.memcached.MemcachedClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for storing HTTP responses in memcached.
 */
@JsonTypeName("memcached")
public class MemcachedResponseStoreConfiguration implements ResponseStoreConfiguration {
    private List<InetSocketAddress> _servers = ImmutableList.of();

    public List<InetSocketAddress> getServers() {
        return _servers;
    }

    public MemcachedResponseStoreConfiguration servers(List<InetSocketAddress> servers) {
        checkNotNull(servers);
        checkArgument(servers.size() > 0, "at least one server address is required");
        _servers = ImmutableList.copyOf(servers);
        return this;
    }

    public MemcachedResponseStoreConfiguration servers(InetSocketAddress... servers) {
        checkNotNull(servers);
        checkArgument(servers.length > 0, "at least one server address is required");
        _servers = ImmutableList.copyOf(servers);
        return this;
    }

    /**
     * InetSocketAddress deserialization is broken in jackson 2.3.3, so using HostAndPort instead.
     * See: https://github.com/FasterXML/jackson-databind/issues/444
     * Fix will be in 2.3.4, but not released yet (as of 2014-Jul-10).
     */
    @JsonProperty
    void setServers(List<HostAndPort> servers) {
        _servers = FluentIterable
                .from(servers)
                .transform(new Function<HostAndPort, InetSocketAddress>() {
                    public InetSocketAddress apply(HostAndPort input) {
                        return new InetSocketAddress(input.getHostText(), input.getPort());
                    }
                })
                .toList();
    }

    @Override
    public ResponseStore createStore() {
        try {
            if (getServers().size() == 0) {
                return ResponseStore.NULL_STORE;
            } else {
                return new MemcachedResponseStore(new MemcachedClient(getServers()));
            }
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }
}
