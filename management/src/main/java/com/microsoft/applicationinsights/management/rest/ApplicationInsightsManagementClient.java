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

package com.microsoft.applicationinsights.management.rest;

import java.io.IOException;
import java.util.List;

import com.microsoft.applicationinsights.management.authentication.Authenticator;
import com.microsoft.applicationinsights.management.authentication.Settings;
import com.microsoft.applicationinsights.management.common.Logger;
import com.microsoft.applicationinsights.management.rest.client.OperationExceptionDetails;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.client.RestClient;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.management.rest.model.ResourceGroup;
import com.microsoft.applicationinsights.management.rest.model.Subscription;
import com.microsoft.applicationinsights.management.rest.model.Tenant;
import com.microsoft.applicationinsights.management.rest.operations.*;
import com.microsoftopentechnologies.auth.AuthenticationContext;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import com.microsoftopentechnologies.auth.browser.BrowserLauncher;

/**
 * Created by yonisha on 4/19/2015.
 *
 * The client to use for Application Insights resource management.
 */
public class ApplicationInsightsManagementClient implements ManagementClient {

    private static final Logger LOG = Logger.getLogger(ApplicationInsightsManagementClient.class.toString());
    private static final String DEFAULT_AUTHENTICATION_TENANT = "common";
    private List<Subscription> authorizedSubscriptions;
    private RestClient restClient;
    private Tenant commonTenant;
    private BrowserLauncher browserLauncher;

    /**
     * Constructs new Application Insights management client.
     * @param authenticationResult The authentication result.
     * @param userAgent The user agent.
     */
    public ApplicationInsightsManagementClient(AuthenticationResult authenticationResult, String userAgent, BrowserLauncher browserLauncher) throws IOException, RestOperationException {
        // Setting the common tenant.
        this.commonTenant = new Tenant();
        this.commonTenant.setId("common");
        this.commonTenant.setAuthenticationToken(authenticationResult);

        this.browserLauncher = browserLauncher;
        this.restClient = new RestClient(userAgent);
        this.authorizedSubscriptions = getAuthorizedSubscriptions();
    }

    /**
     * Gets a list of available subscriptions.
     * @return The list of subscriptions available.
     */
    public List<Subscription> getSubscriptions() throws IOException, RestOperationException {
        return this.authorizedSubscriptions;
    }

    /**
     * Gets a list of resources for a given subscription.
     * @param subscriptionId The subscription ID.
     * @return The resources list.
     */
    public List<Resource> getResources(String subscriptionId) throws IOException, RestOperationException {
        Tenant tenant = getTenantForSubscription(subscriptionId);

        GetResourcesOperation getResourcesOperation = new GetResourcesOperation(tenant, subscriptionId);
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
    public Resource createResource(String subscriptionId, String resourceGroupName, String resourceName, String location) throws IOException, RestOperationException {
        Tenant tenant = getTenantForSubscription(subscriptionId);

        CreateResourceOperation createResourceOperation = new CreateResourceOperation(tenant, subscriptionId, resourceGroupName, resourceName, location);
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
    public ResourceGroup createResourceGroup(String subscriptionId, String resourceGroupName, String location) throws IOException, RestOperationException {
        Tenant tenant = getTenantForSubscription(subscriptionId);

        CreateResourceGroupOperation createResourceGroupOperation = new CreateResourceGroupOperation(tenant, subscriptionId, resourceGroupName, location);
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
    public List<ResourceGroup> getResourceGroups(String subscriptionId) throws IOException, RestOperationException {
        Tenant tenant = getTenantForSubscription(subscriptionId);

        GetResourceGroupsOperation getResourceGroupsOperation = new GetResourceGroupsOperation(tenant, subscriptionId);
        List<ResourceGroup> resourceGroups = getResourceGroupsOperation.execute(this.restClient);

        return resourceGroups;
    }

    /**
     * Gets all the available geo-locations.
     *
     * @return Collection of available geo-locations.
     */
    @Override
    public List<String> getAvailableGeoLocations() throws IOException, RestOperationException {
        Tenant commonTenant = getTenantForSubscription(DEFAULT_AUTHENTICATION_TENANT);

        GetAvailableGeoLocations getAvailableGeoLocations = new GetAvailableGeoLocations(commonTenant);
        List<String> locations = getAvailableGeoLocations.execute(this.restClient);

        return locations;
    }

    private Tenant getTenantForSubscription(String subscriptionId) throws RestOperationException, IOException {
        if (subscriptionId.equalsIgnoreCase(DEFAULT_AUTHENTICATION_TENANT)) {
            return this.commonTenant;
        }

        for (Subscription subscription : this.authorizedSubscriptions) {
            if (subscription.getId().equalsIgnoreCase(subscriptionId)) {
                Tenant tenant = subscription.getTenant();
                renewAccessTokenIfExpired(tenant);

                return tenant;
            }
        }

        String errorMessage = String.format("You not authorized for subscription %s", subscriptionId);
        throw new RestOperationException(errorMessage, new OperationExceptionDetails(errorMessage));
    }

    private List<Subscription> getAuthorizedSubscriptions() throws IOException, RestOperationException {
        LOG.info("Getting available subscriptions.");
        List<Tenant> azureTenants = getAzureTenants();

        GetSubscriptionsOperation getSubscriptionsOperation = new GetSubscriptionsOperation(azureTenants);
        List<Subscription> subscriptions = getSubscriptionsOperation.execute(this.restClient);

        return subscriptions;
    }

    private List<Tenant> getAzureTenants() throws IOException, RestOperationException {
        LOG.info("Getting Azure tenants.");

        GetTenantsOperation getTenantsOperation = new GetTenantsOperation(commonTenant);
        List<Tenant> tenants = getTenantsOperation.execute(this.restClient);

        for (Tenant tenant : tenants) {
            AuthenticationResult authenticationResultForTenant = Authenticator.getAuthenticationResultForTenant(tenant.getId(), this.browserLauncher);
            tenant.setAuthenticationToken(authenticationResultForTenant);
        }

        return tenants;
    }

    private void renewAccessTokenIfExpired(Tenant tenant) throws IOException {
        AuthenticationResult authenticationResult = tenant.getAuthenticationToken();
        if (authenticationResult.getExpiresOn() > 0) {
            return;
        }

        if (authenticationResult.getRefreshToken() == null || authenticationResult.getRefreshToken().equalsIgnoreCase("")) {
            LOG.severe("No refresh token available, cannot renew access token.");

            return;
        }

        LOG.info("Renewing access token for tenant: %s", tenant.getId());

        AuthenticationContext context = new AuthenticationContext(Settings.getAdAuthority());
        context.setBrowserLauncher(this.browserLauncher);
        try {
            authenticationResult = context.acquireTokenByRefreshToken(
                    authenticationResult,
                    DEFAULT_AUTHENTICATION_TENANT,
                    Settings.getAzureServiceManagementUri(),
                    Settings.getClientId());
        } finally {
            context.dispose();
        }

        tenant.setAuthenticationToken(authenticationResult);
    }
}
