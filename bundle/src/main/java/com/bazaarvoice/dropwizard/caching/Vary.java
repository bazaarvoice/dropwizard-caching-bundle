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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the set of request-header fields that fully determines, while the response is fresh,
 * whether a cache is permitted to use the response to reply to a subsequent request without
 * revalidation.
 * <p/>
 * When set, the annotation sets the Vary header on the response.
 * See <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.44">HTTP/1.1 Spec Sec 14.44: Vary</a>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Vary {
    /**
     * Request header fields that determine whether a cache is permitted to use a response to reply
     * to a subsequent request.
     */
    String[] value();
}
