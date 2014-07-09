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
}
