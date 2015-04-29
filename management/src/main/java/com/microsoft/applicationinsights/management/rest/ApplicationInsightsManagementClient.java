package com.microsoft.applicationinsights.management.rest;

import java.io.IOException;
import java.util.List;

import com.microsoft.applicationinsights.management.authentication.Settings;
import com.microsoft.applicationinsights.management.rest.client.RestClient;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.management.rest.model.ResourceGroup;
import com.microsoft.applicationinsights.management.rest.model.Subscription;
import com.microsoft.applicationinsights.management.rest.operations.*;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationContext;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationResult;

/**
 * Created by yonisha on 4/19/2015.
 *
 * The client to use for Application Insights resource management.
 */
public class ApplicationInsightsManagementClient implements ManagementClient {

    private static final String DEFAULT_AUTHENTICATION_TENANT = "common";
    private AuthenticationResult authenticationResult;
    private final String userAgent;
    private RestClient restClient;

    /**
     * Constructs new Application Insights management client.
     * @param authenticationResult The authentication result.
     * @param userAgent The user agent.
     */
    public ApplicationInsightsManagementClient(AuthenticationResult authenticationResult, String userAgent) {
        this.authenticationResult = authenticationResult;
        this.userAgent = userAgent;
        this.restClient = new RestClient(authenticationResult, userAgent);
    }

    /**
     * Gets a list of available subscriptions.
     * @return The list of subscriptions available.
     */
    public List<Subscription> getSubscriptions() throws IOException, AzureCmdException {
        renewAccessTokenIfExpired();

        GetSubscriptionsOperation getSubscriptionsOperation = new GetSubscriptionsOperation();
        List<Subscription> subscriptions = getSubscriptionsOperation.execute(this.restClient);

        return subscriptions;
    }

    /**
     * Gets a list of resources for a given subscription.
     * @param subscriptionId The subscription ID.
     * @return The resources list.
     */
    public List<Resource> getResources(String subscriptionId) throws IOException, AzureCmdException {
        renewAccessTokenIfExpired();

        GetResourcesOperation getResourcesOperation = new GetResourcesOperation(subscriptionId);
        List<Resource> resources = getResourcesOperation.execute(this.restClient);

        return resources;
    }

    /**
     * Creates a new resource.
     * @param subscriptionId The subscription which the resource will be created in.
     * @param resourceGroupName The resource group name.
     * @param resourceName The resource name.
     * @return The resource created.
     */
    public Resource createResource(String subscriptionId, String resourceGroupName, String resourceName) throws IOException, AzureCmdException {
        renewAccessTokenIfExpired();

        CreateResourceOperation createResourceOperation = new CreateResourceOperation(subscriptionId, resourceGroupName, resourceName);
        Resource resource = createResourceOperation.execute(this.restClient);

        return resource;
    }

    /**
     * Create new resources group.
     * @param subscriptionId The subsription ID.
     * @param resourceGroupName The resource group name.
     * @param location The location.
     * @return The new resource group created.
     */
    @Override
    public ResourceGroup createResourceGroup(String subscriptionId, String resourceGroupName, String location) throws IOException, AzureCmdException {
        renewAccessTokenIfExpired();

        CreateResourceGroupOperation createResourceGroupOperation = new CreateResourceGroupOperation(subscriptionId, resourceGroupName, location);
        ResourceGroup resourceGroup = createResourceGroupOperation.execute(this.restClient);

        return resourceGroup;
    }

    /**
     * Gets all resource groups in the given subscription.
     *
     * @param subscriptionId The subscription ID.
     * @return Collection of resource groups.
     */
    @Override
    public List<ResourceGroup> getResourceGroups(String subscriptionId) throws IOException, AzureCmdException {
        GetResourceGroupsOperation getResourceGroupsOperation = new GetResourceGroupsOperation(subscriptionId);
        List<ResourceGroup> resourceGroups = getResourceGroupsOperation.execute(this.restClient);

        return resourceGroups;
    }

    /**
     * Gets all the available geo-locations.
     *
     * @return Collection of available geo-locations.
     */
    @Override
    public List<String> getAvailableGeoLocations() throws IOException, AzureCmdException {
        renewAccessTokenIfExpired();

        GetAvailableGeoLocations getAvailableGeoLocations = new GetAvailableGeoLocations();
        List<String> locations = getAvailableGeoLocations.execute(this.restClient);

        return locations;
    }

    private void renewAccessTokenIfExpired() throws IOException {
        if (this.authenticationResult.getExpiresOn() > 0) {
            return;
        }

        if (this.authenticationResult.getRefreshToken() == null || this.authenticationResult.getRefreshToken().equalsIgnoreCase("")) {
            // TODO: log.

            return;
        }

        AuthenticationContext context = new AuthenticationContext(Settings.getAdAuthority());
        try {
            this.authenticationResult = context.acquireTokenByRefreshToken(
                    this.authenticationResult,
                    DEFAULT_AUTHENTICATION_TENANT,
                    Settings.getAzureServiceManagementUri(),
                    Settings.getClientId());
        } finally {
            context.dispose();
        }

         this.restClient = new RestClient(this.authenticationResult, userAgent);
    }
}
