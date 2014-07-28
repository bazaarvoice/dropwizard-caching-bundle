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
