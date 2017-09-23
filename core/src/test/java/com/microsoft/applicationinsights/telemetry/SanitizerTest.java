/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.util.Sanitizer;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public final class SanitizerTest {
    private final static String VALID_URL = "http://wwww.microsoft.com/";
    private final static String NON_VALID_URL = "http:sd{@~fsd.s.d.f;fffff";

    // No need for UUID tests as we no longer constrain to UUID
//    @Test
//    public void testNonValidEmptyUUID() {
//        boolean valid = Sanitizer.isUUID("");
//        assertFalse(valid);
//    }
//
//    @Test
//    public void testNonValidNullUUID() {
//        boolean valid = Sanitizer.isUUID(null);
//        assertFalse(valid);
//    }
//
//    @Test
//    public void testNonValidBadFormat1UUID() {
//        boolean valid = Sanitizer.isUUID("sadfsa");
//        assertFalse(valid);
//    }
//
//    @Test
//    public void testNonValidBadFormat2UUID() {
//        boolean valid = Sanitizer.isUUID("c934153105ac4d8c972e36e97601d5ffc934153105ac4d8c972e36e97601d5ff");
//        assertFalse(valid);
//    }
//
//    @Test
//    public void testValidUUIDWithComma() {
////      boolean valid = Sanitizer.isUUID("c9341531-05ac-4d8c-972e-36e97601d5ff");
//        boolean valid = Sanitizer.isUUID("00000000-0000-0000-0000-000000000000");
//        assertTrue(valid);
//    }
//
//    @Test
//    public void testValidUUIDWithoutComma() {
//        boolean valid = Sanitizer.isUUID("c934153105ac4d8c972e36e97601d5ff");
//        assertFalse(valid);
//    }


    @Test
    public void testSanitizeNullUri() throws Exception {
        Sanitizer.sanitizeUri(null);
    }

    @Test
    public void testSanitizeNonValidUri() throws Exception {
        URI uri = Sanitizer.sanitizeUri(NON_VALID_URL);
        assertNull(uri);
    }

    @Test
    public void testSanitizeValidUri() throws Exception {
        URI uri = Sanitizer.sanitizeUri(VALID_URL);
        assertNotNull(uri);
    }

    @Test
    public void testSafeStringToUrlWithNull() throws Exception {
        URI url = Sanitizer.safeStringToUri(null);
        assertNull(url);
    }

    @Test
    public void testSafeStringToUrlWithEmpty() throws Exception {
        URI uri = Sanitizer.sanitizeUri("");
        assertNull(uri);
    }

    @Test
    public void testSafeStringToUrlWithValid() throws Exception {
        URI uri = Sanitizer.sanitizeUri(VALID_URL);
        assertNotNull(uri);
    }

    @Test
    public void testSafeStringToUrlWithNonValid() throws Exception {
        URI uri = Sanitizer.sanitizeUri(NON_VALID_URL);
        assertNull(uri);
    }

}
