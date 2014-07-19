package com.bazaarvoice.dropwizard.caching.memcached;

import net.spy.memcached.MemcachedClient;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link MemcachedResponseStore}.
 */
public class MemcachedResponseStoreTest {
    @DataProvider
    public static Object[][] invalidateData() {
        return new Object[][]{
                {"", false, "key", "key"},
                {"", true, "key", null},
                {"pre", false, "key", "prekey"},
                {"pre", true, "key", null}
        };
    }

    @Test(dataProvider = "invalidateData")
    public void invalidate(String prefix, boolean readOnly, String key, String memcacheKey) {
        MemcachedClient client = mock(MemcachedClient.class);
        MemcachedResponseStore store = new MemcachedResponseStore(client, prefix, readOnly);
        store.invalidate(key);

        if (memcacheKey != null) {
            verify(client).delete(memcacheKey);
        }

        verifyNoMoreInteractions(client);
    }
}
