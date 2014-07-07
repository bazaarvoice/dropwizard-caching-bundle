package com.bazaarvoice.dropwizard.caching;

import javax.ws.rs.core.CacheControl;

class CacheControlUtils {
    /**
     * Get the max age for a shared cache.
     *
     * @param cacheControl cache-control header
     * @return s-maxage, if set, otherwise maxage in seconds or -1 if neither is set
     */
    public static int getSharedCacheMaxAge(CacheControl cacheControl) {
        int maxAge = cacheControl.getSMaxAge();

        if (maxAge < 0) {
            maxAge = cacheControl.getMaxAge();
        }

        return Math.max(maxAge, -1);
    }

    /**
     * Determine if the response can be stored in the cache.
     *
     * @param cacheConfig response cache-control header
     * @return true if the response can potentially be cached, false if the response can not be cached
     */
    public static boolean isResponseCacheable(CacheControl cacheConfig) {
        return !cacheConfig.isPrivate() &&
                !cacheConfig.isNoStore() &&
                !cacheConfig.isNoCache() &&
                getSharedCacheMaxAge(cacheConfig) != 0;

    }

    /**
     * Determine if the client is requiring fresh data (request must go to origin) or if the response may be served
     * from the cache.
     *
     * @param cacheConfig request cache-control header
     * @return true if the response can be served from cache, false if the request must go to origin
     */
    public static boolean isRequestServableFromCache(CacheControl cacheConfig) {
        return !cacheConfig.isNoCache() &&
                !cacheConfig.isPrivate() &&
                getSharedCacheMaxAge(cacheConfig) != 0;
    }

    /**
     * Determine if the client is requiring fresh data and that the response not be cached.
     * <p/>
     * This allows an optimization where the caching code can be completely bypassed.
     *
     * @param cacheConfig request cache-control header
     * @return true if the request must not be served from the cache and the response must not be cached
     */
    public static boolean isRequestIgnoreCache(CacheControl cacheConfig) {
        return (cacheConfig.isNoCache() && cacheConfig.isNoStore()) ||
                cacheConfig.isPrivate();
    }
}
