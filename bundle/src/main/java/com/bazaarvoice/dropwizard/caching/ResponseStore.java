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

import com.google.common.base.Optional;

/**
 * Shared response cache.
 */
public abstract class ResponseStore {
    public static final ResponseStore NULL_STORE = new ResponseStore() {
        @Override
        public Optional<CachedResponse> get(String key) {
            return Optional.absent();
        }

        @Override
        public void put(String key, CachedResponse response) {
            // Do nothing
        }

        @Override
        public void invalidate(String key) {
            // Do nothing
        }
    };

    public abstract Optional<CachedResponse> get(String key);

    public abstract void put(String key, CachedResponse response);

    public abstract void invalidate(String key);
}
