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

import com.bazaarvoice.dropwizard.caching.ResponseStore;
import com.bazaarvoice.dropwizard.caching.ResponseStoreFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for storing HTTP responses in memcached.
 */
@JsonTypeName("memcached")
public class MemcachedResponseStoreFactory implements ResponseStoreFactory {
    private List<InetSocketAddress> _servers = ImmutableList.of();
    private String _keyPrefix = "";
    private boolean _readOnly;

    public boolean isReadOnly() {
        return _readOnly;
    }

    @JsonProperty
    public void setReadOnly(boolean readOnly) {
        _readOnly = readOnly;
    }

    public String getKeyPrefix() {
        return _keyPrefix;
    }

    @JsonProperty
    public void setKeyPrefix(String keyPrefix) {
        _keyPrefix = checkNotNull(keyPrefix);
    }

    public List<InetSocketAddress> getServers() {
        return _servers;
    }

    @JsonIgnore
    public void setServers(List<InetSocketAddress> servers) {
        checkNotNull(servers);
        _servers = ImmutableList.copyOf(servers);
    }

    /**
     * InetSocketAddress deserialization is broken in jackson 2.3.3, so using HostAndPort instead.
     * See: https://github.com/FasterXML/jackson-databind/issues/444
     * Fix will be in 2.3.4, but not released yet (as of 2014-Jul-10).
     */
    @JsonProperty
    void setServers(HostAndPort[] servers) {
        checkNotNull(servers);

        _servers = FluentIterable
                .from(Arrays.asList(servers))
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
                MemcachedClient client = new MemcachedClient(new BinaryConnectionFactory(), getServers());
                return new MemcachedResponseStore(client, _keyPrefix, _readOnly);
            }
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }
}
