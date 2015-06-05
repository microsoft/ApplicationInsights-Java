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

package com.microsoft.applicationinsights.internal.channel.common;

import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;
import com.microsoft.applicationinsights.internal.reflect.ClassDataVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;

public final class ApacheSenderFactoryTest {
    @Test
    public void testOldVersion() throws NoSuchFieldException, IllegalAccessException {
        ApacheSender sender = createApacheSender(false);

        assertEquals(sender.getHttpClient().getClass(), DefaultHttpClient.class);
    }

    @Test
    public void testNewVersion() throws NoSuchFieldException, IllegalAccessException {
        ApacheSender sender = createApacheSender(true);

        CloseableHttpClient httpClient = (CloseableHttpClient)sender.getHttpClient();
        assertNotNull(httpClient);
    }

    private static ApacheSender createApacheSender(boolean isNewVersion) throws NoSuchFieldException, IllegalAccessException {
        Field field = ClassDataUtils.class.getDeclaredField("verifier");
        field.setAccessible(true);

        ClassDataVerifier mockVerifier = Mockito.mock(ClassDataVerifier.class);
        Mockito.doReturn(isNewVersion).when(mockVerifier).verifyClassExists(anyString());
        field.set(ClassDataUtils.INSTANCE, mockVerifier);

        ApacheSender sender = new ApacheSenderFactory().create();
        assertNotNull(sender);
        return sender;
    }
}
