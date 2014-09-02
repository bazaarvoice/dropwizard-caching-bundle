package com.bazaarvoice.dropwizard.caching;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a resource includes this annotation, the request body content will be included as part of the
 * cache key.
 * <p/>
 * Only requests with exactly the same body content as a previously cached request will receive the
 * cached response. If the request body differs, the request will be revalidated with the origin
 * server.
 * <p/>
 * This will result in non-standard behavior and upstream caches will most likely not support it.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IncludeBodyInCacheKey {
    boolean enabled() default true;
}
