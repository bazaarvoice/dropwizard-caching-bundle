package com.bazaarvoice.dropwizard.caching.memcached;

import com.bazaarvoice.dropwizard.caching.CachedResponse;
import com.google.common.base.Charsets;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;
import com.sun.jersey.core.util.UnmodifiableMultivaluedMap;
import net.spy.memcached.CachedData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedMap;

import static org.testng.Assert.assertEquals;
import static com.google.common.net.HttpHeaders.DATE;

/**
 * Tests for {@link CachedResponseTranscoder}.
 */
public class CachedResponseTranscoderTest {
    @DataProvider
    public Object[][] encodeData() {
        return new Object[][]{
                {response(200, headers(), bytes()),
                        bytes("HTTP/1.1 200 \r\n\r\n")},

                {response(300, headers(), bytes()),
                        bytes("HTTP/1.1 300 \r\n\r\n")},

                {response(200, headers(), bytes("{\"key\":\"value\"}")),
                        bytes("HTTP/1.1 200 \r\n\r\n{\"key\":\"value\"}")},

                {response(300, headers(), bytes()),
                        bytes("HTTP/1.1 300 \r\n\r\n")},

                {response(200, headers(DATE, "Thu, 12 Sep 2013 11:41:52 GMT"), bytes()),
                        bytes("HTTP/1.1 200 \r\nDate: Thu, 12 Sep 2013 11:41:52 GMT\r\n\r\n")},

                {response(200, headers(DATE, "Thu, 12 Sep 2013 11:41:52 GMT"), bytes("{\"key\":\"value\"}")),
                        bytes("HTTP/1.1 200 \r\nDate: Thu, 12 Sep 2013 11:41:52 GMT\r\n\r\n{\"key\":\"value\"}")}
        };
    }

    @Test(dataProvider = "encodeData")
    public void encode(CachedResponse response, byte[] encoded) {
        CachedData data = CachedResponseTranscoder.INSTANCE.encode(response);
        assertEquals(data.getData(), encoded);
        assertEquals(data.getFlags(), 0);
    }

    @Test(dataProvider = "encodeData")
    public void decode(CachedResponse response, byte[] encoded) {
        CachedResponse decoded = CachedResponseTranscoder.INSTANCE.decode(new CachedData(0, encoded, CachedResponseTranscoder.INSTANCE.getMaxSize()));
        assertEquals(decoded, response);
    }

    private static CachedResponse response(int code, MultivaluedMap<String, String> headers, byte[] content) {
        return new CachedResponse(code, headers, content);
    }

    private static MultivaluedMap<String, String> headers() {
        return new UnmodifiableMultivaluedMap<String, String>(new MultivaluedMapImpl());
    }

    private static MultivaluedMap<String, String> headers(String key1, String value1) {
        MultivaluedMap<String, String> headers = new StringKeyIgnoreCaseMultivaluedMap<String>();
        headers.add(key1, value1);
        return new UnmodifiableMultivaluedMap<String, String>(headers);
    }

    private static byte[] bytes() {
        return new byte[0];
    }

    private static byte[] bytes(String value) {
        return value.getBytes(Charsets.UTF_8);
    }
}
