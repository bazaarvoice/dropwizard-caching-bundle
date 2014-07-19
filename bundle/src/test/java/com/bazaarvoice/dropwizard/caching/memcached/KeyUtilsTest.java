package com.bazaarvoice.dropwizard.caching.memcached;

import com.google.common.base.Strings;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link KeyUtils}.
 */
public class KeyUtilsTest {
    @DataProvider
    public Object[][] truncateKeyData() {
        return new Object[][]{
                {"a", null},
                {repeat('a', 250), null},
                {repeat('a', 251), repeat('a', 222) + "vscbJ6aycQ3t8dcTXEf0UGBR8Ik="},
                {repeat('a', 249) + "=", repeat('a', 222) + "JeE7ipIsBmXczPVeGjDhfdJLppw="},
                {repeat('a', 220) + "\u20AC" + repeat('a', 28), repeat('a', 220) + "++1ojPGEGnJTllsG+EUY73md7Z9yw="}
        };
    }

    @Test(dataProvider = "truncateKeyData")
    public void truncateKey(String key, String expectedResult) {
        assertEquals(KeyUtils.truncateKey(key), expectedResult == null ? key : expectedResult);
    }

    private static String repeat(char x, int length) {
        return Strings.padEnd("", length, x);
    }
}
