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
