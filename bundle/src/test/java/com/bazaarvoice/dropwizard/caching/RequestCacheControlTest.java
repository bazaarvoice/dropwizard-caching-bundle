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
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link RequestCacheControl}.
 */
public class RequestCacheControlTest {
    @Test
    public void test() {
        DateTime date = DateTime.now();
        DateTime now = date.plusSeconds(20);

        System.out.println(Seconds.secondsBetween(date, now).getSeconds());
        System.out.println(Seconds.secondsBetween(now, date).getSeconds());
    }

    @DataProvider
    public static Object[][] valueOfData() {
        return new Object[][]{
                {"", false, false, false, false, -1, -1, -1, null},
                {"no-cache", true, false, false, false, -1, -1, -1, null},
                {"max-stale", false, false, false, false, -1, Integer.MAX_VALUE, -1, null},
                {"no-cache, no-store, no-transform, only-if-cached, max-age=0, max-stale=0, min-fresh=0", true, true, true, true, 0, 0, 0, null},
                {"unknown, unknown-value=xxx, unknown-quoted=\"yyy  zzz\"", false, false, false, false, -1, -1, -1, ImmutableMap.of(
                        "unknown", Optional.<String>absent(),
                        "unknown-value", Optional.of("xxx"),
                        "unknown-quoted", Optional.of("yyy  zzz")
                )}
        };
    }

    @Test(dataProvider = "valueOfData")
    public void valueOf(String value, boolean noCache, boolean noStore, boolean noTransform, boolean onlyIfCached, int maxAge, int maxStale, int minFresh, Map<String, Optional<String>> cacheExtension) {
        RequestCacheControl cacheControl = RequestCacheControl.valueOf(value);
        assertEquals(cacheControl.isNoCache(), noCache);
        assertEquals(cacheControl.isNoStore(), noStore);
        assertEquals(cacheControl.isNoTransform(), noTransform);
        assertEquals(cacheControl.isOnlyIfCached(), onlyIfCached);
        assertEquals(cacheControl.getCacheExtension(), cacheExtension == null ? ImmutableMap.of() : cacheExtension);
        assertEquals(cacheControl.getMaxAge(), maxAge);
        assertEquals(cacheControl.getMaxStale(), maxStale);
        assertEquals(cacheControl.getMinFresh(), minFresh);
    }

    @DataProvider
    public static Object[][] toStringData() {
        return new Object[][]{
                {""},
                {"no-cache"},
                {"max-stale"},
                {"no-cache, no-store, max-age=1, max-stale=1, min-fresh=1, no-transform, only-if-cached"},
                {"unknown, unknown-value=xxx, unknown-quoted=\"yyy  zzz\""}
        };
    }

    @Test(dataProvider = "toStringData")
    public void testToString(String value) {
        assertEquals(RequestCacheControl.valueOf(value).toString(), value);
    }
}
