package com.bazaarvoice.dropwizard.caching;

/**
 * Standard cache-control header directives that can appear with no value.
 */
public enum CacheControlFlag {
    NO_CACHE,
    NO_STORE,
    MUST_REVALIDATE,
    PROXY_REVALIDATE,
    NO_TRANSFORM,
    PRIVATE,
    PUBLIC
}
