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
package com.bazaarvoice.dropwizard.caching.example;

import com.bazaarvoice.dropwizard.caching.CacheControlConfiguration;
import com.bazaarvoice.dropwizard.caching.CachingBundleConfiguration;
import com.bazaarvoice.dropwizard.caching.CachingConfiguration;
import io.dropwizard.Configuration;

public class ExampleConfiguration extends Configuration implements CachingBundleConfiguration {
    private CachingConfiguration cache = new CachingConfiguration();
    public CacheControlConfiguration cacheControl = new CacheControlConfiguration();

    public CachingConfiguration getCache() {
        return cache;
    }

    public CacheControlConfiguration getCacheControl() {
        return cacheControl;
    }
}
