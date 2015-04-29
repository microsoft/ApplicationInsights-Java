package com.microsoft.applicationinsights.management.rest.operations;

import com.microsoft.applicationinsights.management.rest.client.HttpMethod;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.Resource;
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
    public void testInitialize() throws IOException, AzureCmdException {
        restClient = mock(Client.class);
        Mockito.doReturn(resourcesJson).when(restClient).executeGet(Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testRestClientCalledWhenExecuting() throws IOException, AzureCmdException {
        GetResourcesOperation getResourcesOperation = new GetResourcesOperation(SUBSCRIPTION_ID);
        getResourcesOperation.execute(restClient);

        verify(restClient, times(1)).executeGet(Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testResourcesJsonParsedCorrectly() throws IOException, AzureCmdException {
        GetResourcesOperation getResourcesOperation = new GetResourcesOperation(SUBSCRIPTION_ID);
        List<Resource> resources = getResourcesOperation.execute(restClient);

        Assert.assertEquals(1, resources.size());
        Resource resource = resources.get(0);
        Assert.assertEquals(resourceId, resource.getId().toString());
        Assert.assertEquals(RESOURCE_NAME, resource.getName());
    }
}
