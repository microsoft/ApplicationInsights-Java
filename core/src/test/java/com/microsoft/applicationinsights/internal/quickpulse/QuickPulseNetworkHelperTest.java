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

package com.microsoft.applicationinsights.internal.quickpulse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Test;
import org.mockito.Mockito;

/** Created by gupele on 12/15/2016. */
public class QuickPulseNetworkHelperTest {
  @Test
  public void testIsSuccessWith200() {
    final HttpResponse response = mock(HttpResponse.class);
    final StatusLine statusLine = mock(StatusLine.class);

    Mockito.doReturn(statusLine).when(response).getStatusLine();
    Mockito.doReturn(200).when(statusLine).getStatusCode();

    final boolean result = new QuickPulseNetworkHelper().isSuccess(response);
    assertTrue(result);
  }

  @Test
  public void testIsSuccessWith500() {
    final HttpResponse response = mock(HttpResponse.class);
    final StatusLine statusLine = mock(StatusLine.class);

    Mockito.doReturn(statusLine).when(response).getStatusLine();
    Mockito.doReturn(500).when(statusLine).getStatusCode();

    final boolean result = new QuickPulseNetworkHelper().isSuccess(response);
    assertFalse(result);
  }
}
