package com.bazaarvoice.dropwizard.caching.example;

import com.bazaarvoice.dropwizard.caching.CachingBundleConfiguration;
import com.bazaarvoice.dropwizard.caching.CachingConfiguration;
import io.dropwizard.Configuration;

public class ExampleConfiguration extends Configuration implements CachingBundleConfiguration {
    private CachingConfiguration cache = new CachingConfiguration();

    public CachingConfiguration getCache() {
        return cache;
    }
}
