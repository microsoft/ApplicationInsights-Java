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

package com.microsoft.applicationinsights.management.rest.operations;

import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.model.Tenant;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by yonisha on 6/15/2015.
 */
public class CreateResourceOperationTests {
    private final String DEFAULT_RESOURCE_GROUP = "resource_group";
    private final String DEFAULT_RESOURCE_NAME = "resource_name";
    private final String DEFAULT_LOCATION = "central us";
    private final String DEFAULT_SUBSCRIPTION_ID = "subscription_id";
    private Client restClient = mock(Client.class);

    @Test
    public void testCreateResource() throws IOException, RestOperationException {
        CreateResourceOperation createResourceOperation =
                new CreateResourceOperation(new Tenant(), DEFAULT_SUBSCRIPTION_ID, DEFAULT_RESOURCE_GROUP, DEFAULT_RESOURCE_NAME, DEFAULT_LOCATION);
        createResourceOperation.execute(this.restClient);

        verify(restClient).executePut(Matchers.any(Tenant.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
    }
}
