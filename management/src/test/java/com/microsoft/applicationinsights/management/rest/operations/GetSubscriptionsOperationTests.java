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
