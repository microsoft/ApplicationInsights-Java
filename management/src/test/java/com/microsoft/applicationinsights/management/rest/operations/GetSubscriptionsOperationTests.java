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
import com.microsoft.applicationinsights.management.rest.model.Subscription;
import com.microsoft.applicationinsights.management.rest.model.Tenant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by yonisha on 4/19/2015.
 */
public class GetSubscriptionsOperationTests {

    private static List<Tenant> defaultTenantList = new ArrayList<Tenant>();
    private static final String SUBSCRIPTION_ID = "11111111-6163-4ae6-9b38-9f10c1429f24";
    private static final String SUBSCRIPTION_DISPLAY_NAME = "SubscriptionDisplayName";
    private static Client restClient;
    private static final String subscriptionJson =
            String.format("{\"value\":[{\"id\":\"/subscriptions/11111111-6163-4ae6-9b38-9f10c1429f24\",\"subscriptionId\":\"%s\",\"displayName\":\"%s\",\"state\":\"Enabled\",\"subscriptionPolicies\":{\"locationPlacementId\":\"Internal_2014-09-01\",\"quotaId\":\"Internal_2014-09-01\"}}]}", SUBSCRIPTION_ID, SUBSCRIPTION_DISPLAY_NAME);

    @BeforeClass
    public static void classInitialize() {
        Tenant defaultTenant = new Tenant();
        defaultTenantList.add(defaultTenant);
    }

    @Before
    public void testInitialize() throws IOException, RestOperationException {
        restClient = mock(Client.class);
        Mockito.doReturn(subscriptionJson).when(restClient).executeGet(Matchers.any(Tenant.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testRestClientCalledWhenExecuting() throws IOException, RestOperationException {
        GetSubscriptionsOperation getSubscriptionsOperation = new GetSubscriptionsOperation(defaultTenantList);
        getSubscriptionsOperation.execute(restClient);

        verify(restClient, times(1)).executeGet(Matchers.any(Tenant.class), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testSubscriptionsJsonParsedCorrectly() throws IOException, RestOperationException {
        GetSubscriptionsOperation getSubscriptionsOperation = new GetSubscriptionsOperation(defaultTenantList);
        List<Subscription> subscriptions = getSubscriptionsOperation.execute(restClient);

        Assert.assertEquals(1, subscriptions.size());

        Subscription subscription = subscriptions.get(0);
        Assert.assertEquals(SUBSCRIPTION_ID, subscription.getId().toString());
        Assert.assertEquals(SUBSCRIPTION_DISPLAY_NAME, subscription.getName());
    }
}
