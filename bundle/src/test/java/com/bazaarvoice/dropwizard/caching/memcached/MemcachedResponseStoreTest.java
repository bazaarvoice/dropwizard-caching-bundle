package com.bazaarvoice.dropwizard.caching.memcached;

import com.bazaarvoice.dropwizard.caching.CachedResponse;
import com.google.common.base.Optional;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.Transcoder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;

/**
 * Tests for {@link MemcachedResponseStore}.
 */
public class MemcachedResponseStoreTest {
    @DataProvider
    public static Object[][] invalidateData() {
        return new Object[][]{
                {"", false, "key", "key"},
                {"", true, "key", "key"},
                {"pre", false, "key", "prekey"},
                {"pre", true, "key", "prekey"}
        };
    }

    @Test(dataProvider = "invalidateData")
    public void invalidate(String prefix, boolean readOnly, String key, String memcacheKey) {
        MemcachedClient client = mock(MemcachedClient.class);
        MemcachedResponseStore store = new MemcachedResponseStore(client, prefix, readOnly);
        store.invalidate(key);

        if (!readOnly) {
            verify(client).delete(memcacheKey);
        }

        verifyNoMoreInteractions(client);
    }

    @DataProvider
    public static Object[][] putData() {
        return new Object[][]{
                {"", false, "key", "key", 1388534400, response(new DateTime(2014, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC))},
                {"", true, "key", "key", 1388534400, response(new DateTime(2014, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC))},
                {"pre", false, "key", "prekey", 1388534400, response(new DateTime(2014, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC))},
                {"pre", true, "key", "prekey", 1388534400, response(new DateTime(2014, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC))}
        };
    }

    @Test(dataProvider = "putData")
    public void put(String prefix, boolean readOnly, String key, String memcacheKey, int memcacheExpiration, CachedResponse value) {
        MemcachedClient client = mock(MemcachedClient.class);
        MemcachedResponseStore store = new MemcachedResponseStore(client, prefix, readOnly);
        store.put(key, value);

        if (!readOnly) {
            verify(client).set(eq(memcacheKey), eq(memcacheExpiration), same(value), any(Transcoder.class));
        }

        verifyNoMoreInteractions(client);
    }

    @Test
    public void put_with_no_expiration() {
        MemcachedClient client = mock(MemcachedClient.class);
        MemcachedResponseStore store = new MemcachedResponseStore(client, "", false);
        store.put("key", response(null));
        verifyNoMoreInteractions(client);
    }

    @DataProvider
    public static Object[][] getData() {
        return new Object[][]{
                {"", false, "key", "key", null},
                {"", true, "key", "key", null},
                {"pre", false, "key", "prekey", null},
                {"pre", true, "key", "prekey", null},

                {"", false, "key", "key", mock(CachedResponse.class)},
                {"", true, "key", "key", mock(CachedResponse.class)},
                {"pre", false, "key", "prekey", mock(CachedResponse.class)},
                {"pre", true, "key", "prekey", mock(CachedResponse.class)}
        };
    }

    @Test(dataProvider = "getData")
    public void get(String prefix, boolean readOnly, String key, String memcacheKey, CachedResponse response) {
        MemcachedClient client = mock(MemcachedClient.class);
        MemcachedResponseStore store = new MemcachedResponseStore(client, prefix, readOnly);

        when(client.get(eq(memcacheKey), any(Transcoder.class))).thenReturn(response);

        Optional<CachedResponse> getResponse = store.get(key);

        if (response == null) {
            assertFalse(getResponse.isPresent());
        } else {
            assertSame(getResponse.get(), response);
        }

        verify(client).get(eq(memcacheKey), any(Transcoder.class));
    }

    private static CachedResponse response(DateTime expires) {
        CachedResponse response = mock(CachedResponse.class);
        when(response.getExpires()).thenReturn(Optional.fromNullable(expires));
        return response;
    }
}
