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

import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.management.rest.model.Tenant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by yonisha on 4/19/2015.
 */
public class GetResourcesOperationTests {
    private static final String SUBSCRIPTION_ID = "11111111-6163-4ae6-9b38-9f10c1429f24";
    private static final String RESOURCE_NAME = "yonishatest";

    private static Client restClient;
    private static String resourceId =
            String.format("/subscriptions/%s/resourceGroups/Group-18/providers/microsoft.insights/components/%s", SUBSCRIPTION_ID, RESOURCE_NAME);
    private static final String resourcesJson =
            String.format("{\"value\":[{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"microsoft.insights/components\",\"location\":\"centralus\",\"tags\":{},\"properties\":{\"TenantId\":\"11111111-e6a6-47be-a6d5-dc87b947286a\",\"Application_Type\":\"web\",\"Flow_Type\":\"Redfield\",\"Request_Source\":\"IbizaAIExtension\",\"InstrumentationKey\":\"11111111-5d7d-4bb0-bf14-33d9c556be96\",\"Name\":\"yonishatest\",\"CreationDate\":\"2015-01-25T10:55:06.3886179+00:00\",\"PackageId\":null,\"ApplicationId\":\"yonishatest\"}}],\"nextLink\":null}", resourceId, RESOURCE_NAME);

    @Before
    public void testInitialize() throws IOException, RestOperationException {
        restClient = mock(Client.class);
        Mockito.doReturn(resourcesJson).when(restClient).executeGet(Matchers.any(Tenant.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testRestClientCalledWhenExecuting() throws IOException, RestOperationException {
        GetResourcesOperation getResourcesOperation = new GetResourcesOperation(new Tenant(), SUBSCRIPTION_ID);
        getResourcesOperation.execute(restClient);

        verify(restClient, times(1)).executeGet(Matchers.any(Tenant.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testResourcesJsonParsedCorrectly() throws IOException, RestOperationException {
        GetResourcesOperation getResourcesOperation = new GetResourcesOperation(new Tenant(), SUBSCRIPTION_ID);
        List<Resource> resources = getResourcesOperation.execute(restClient);

        Assert.assertEquals(1, resources.size());
        Resource resource = resources.get(0);
        Assert.assertEquals(resourceId, resource.getId().toString());
        Assert.assertEquals(RESOURCE_NAME, resource.getName());
    }
}
