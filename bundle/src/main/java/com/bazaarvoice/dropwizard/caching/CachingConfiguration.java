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
package com.bazaarvoice.dropwizard.caching;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration options for {@link CachingBundle}.
 */
public class CachingConfiguration {
    private LocalCacheConfiguration _local = new LocalCacheConfiguration();
    private ResponseStoreFactory _storeFactory = ResponseStoreFactory.NULL_STORE_FACTORY;

    public LocalCacheConfiguration getLocal() {
        return _local;
    }

    @JsonProperty
    public void setLocal(LocalCacheConfiguration local) {
        _local = checkNotNull(local);
    }

    public ResponseStoreFactory getStoreFactory() {
        return _storeFactory;
    }

    @JsonProperty("store")
    public void setStoreFactory(ResponseStoreFactory storeFactory) {
        _storeFactory = checkNotNull(storeFactory);
    }

    public ResponseCache buildCache(MetricRegistry metricRegistry) {
        return new ResponseCache(_local.buildCache(), _storeFactory.createStore(), metricRegistry);
    }
}
