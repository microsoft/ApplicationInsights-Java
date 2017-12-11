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

package com.microsoft.applicationinsights.web.internal.correlation.mocks;

import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpUriRequest;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockHttpAsyncClientWrapper {

    private final HttpAsyncClient mockClient;
    private final MockHttpEntity entity;
    private final MockHttpResponse response;
    private final MockHttpTask task;

    public MockHttpAsyncClientWrapper() {
        
        this.entity = new MockHttpEntity();
        this.response = new MockHttpResponse(this.entity, 200);
        
        this.task = new MockHttpTask(this.response);

        this.mockClient = mock(HttpAsyncClient.class);
        
        when(mockClient.execute(any(HttpUriRequest.class), any())).thenReturn(this.task);
    }

    public void setAppId(String appId) {
        this.entity.setContent(appId);
    }

    public void setFailureOn(boolean fail) {
        this.task.setFailureOn(fail);
    }

    public void setTaskAsComplete() {
        this.task.setIsDone(true);
    }

    public void setTaskAsPending() {
        this.task.setIsDone(false);
    }

    public void setStatusCode(int code) {
        this.response.setStatusCode(code);
    }

    public HttpAsyncClient getClient() {
        return this.mockClient;
    }
}