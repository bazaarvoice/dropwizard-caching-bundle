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
