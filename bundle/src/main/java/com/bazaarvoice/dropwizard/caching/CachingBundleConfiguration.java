package com.bazaarvoice.dropwizard.caching;

/**
 * Interface that must be implemented by application configuration class that loads the {@link CachingBundle}.
 */
public interface CachingBundleConfiguration {
    CachingConfiguration getCache();
    CacheControlConfiguration getCacheControl();
}
