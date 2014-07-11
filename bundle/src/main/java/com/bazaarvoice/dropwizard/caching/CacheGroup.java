package com.bazaarvoice.dropwizard.caching;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the cache group a resource method belongs to so common cache control settings can be enabled.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheGroup {
    /**
     * Name of the cache group to associate with the resource method.
     */
    String value();
}
