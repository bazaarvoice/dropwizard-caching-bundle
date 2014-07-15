package com.bazaarvoice.dropwizard.caching.example;

import com.bazaarvoice.dropwizard.caching.CacheControlConfiguration;
import com.bazaarvoice.dropwizard.caching.CachingBundleConfiguration;
import com.bazaarvoice.dropwizard.caching.CachingConfiguration;
import com.yammer.dropwizard.config.Configuration;

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
