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

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link com.bazaarvoice.dropwizard.caching.CacheRequestContext}.
 */
public class CacheRequestContextTest {
    @Test
    public void constructor() {
        CacheRequestContext requestContext = new CacheRequestContext("GET", URI.create("http://host/the/path"), new MultivaluedMapImpl(), "abc");
        assertEquals(requestContext.getRequestHash(), "abc");
        assertEquals(requestContext.getRequestMethod(), "GET");
        assertEquals(requestContext.getRequestUri(), URI.create("http://host/the/path"));
    }

    @DataProvider
    public Object[][] pragmaNoCacheData() {
        return new Object[][]{
                {buildMap(), false},
                {buildMap("Pragma", newArrayList("directive")), false},
                {buildMap("Pragma", newArrayList("no-cache")), true},
                {buildMap("Pragma", newArrayList("directive", "other=\"the value\""), "Pragma", newArrayList("no-cache")), true},
        };
    }

    @Test(dataProvider = "pragmaNoCacheData")
    public void pragmaNoCache(MultivaluedMap<String, String> headers, boolean isPragmaNoCache) {
        CacheRequestContext requestContext = new CacheRequestContext("GET", URI.create("http://host"), headers, "abc");
        assertEquals(requestContext.isPragmaNoCache(), isPragmaNoCache);
    }

    @DataProvider
    public Object[][] getCacheControlData() {
        return new Object[][]{
                {buildMap(), ""},
                {buildMap("Stuff", newArrayList("value")), ""},
                {buildMap("Cache-Control", newArrayList("no-cache")), "no-cache"},
                {buildMap("Cache-Control", newArrayList("no-store", "max-age=5", "max-age=10")), "no-store, max-age=10"},
                {buildMap("Cache-Control", newArrayList("invalid=\"no end quote", "must-revalidate")), ""}
        };
    }

    @Test(dataProvider = "getCacheControlData")
    public void getCacheControl(MultivaluedMap<String, String> headers, String cacheControl) {
        CacheRequestContext requestContext = new CacheRequestContext("GET", URI.create("http://host"), headers, "abc");
        assertEquals(requestContext.getCacheControl(), RequestCacheControl.valueOf(cacheControl));
    }

    private MultivaluedMap<String, String> buildMap() {
        return new MultivaluedMapImpl();
    }

    private MultivaluedMap<String, String> buildMap(String key, List<String> value) {
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.put(key, value);
        return map;
    }

    private MultivaluedMap<String, String> buildMap(String key1, List<String> value1, String key2, List<String> value2) {
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }
}
