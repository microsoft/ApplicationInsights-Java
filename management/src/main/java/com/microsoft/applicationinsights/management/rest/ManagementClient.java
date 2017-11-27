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

import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.management.rest.model.ResourceGroup;
import com.microsoft.applicationinsights.management.rest.model.Subscription;
import com.microsoft.applicationinsights.management.rest.model.Tenant;

/**
 * Created by yonisha on 4/21/2015.
 */
public interface ManagementClient {

    // region Subscriptions

    /**
     * Gets a list of available subscriptions.
     * @return The list of subscriptions available.
     */
    List<Subscription> getSubscriptions() throws IOException, RestOperationException;

    // endregion Subscriptions

    // region Resources

    /**
     * Gets a list of resources for a given subscription.
     * @param subscriptionId The subscription ID.
     * @return The resources list.
     */
    List<Resource> getResources(String subscriptionId) throws IOException, RestOperationException;

    /**
     * Creates a new resource.
     * @param subscriptionId The subscription which the resource will be created in.
     * @param resourceGroupName The resource group name.
     * @param resourceName The resource name.
     * @param location The location is to create the resource in.
     * @return The resource created.
     */
    Resource createResource(String subscriptionId, String resourceGroupName, String resourceName, String location) throws IOException, RestOperationException;

    // endregion Resources

    // region Resource Groups

    /**
     * Creates new resource group.
     * @param subscriptionId The subsription ID.
     * @param resourceGroupName The resource group name.
     * @param location The location.
     * @return The new resource group created.
     */
    ResourceGroup createResourceGroup(String subscriptionId, String resourceGroupName, String location) throws IOException, RestOperationException;

    /**
     * Gets all resource groups in the given subscription.
     * @param subscriptionId The subscription ID.
     * @return Collection of resource groups.
     */
    List<ResourceGroup> getResourceGroups(String subscriptionId) throws IOException, RestOperationException;

    // endregion Resource Groups

    // region Geo Locations

    /**
     * Gets all the available geo-locations.
     * @return Collection of available geo-locations.
     */
    List<String> getAvailableGeoLocations() throws IOException, RestOperationException;

    /**
     * Gets all the available geo-locations for a subscription id.
     * @param subscriptionId The subscription ID.
     * @return Collection of available geo-locations.
     */
    List<String> getAvailableGeoLocations(String subscriptionId) throws IOException, RestOperationException;

    // endregion Geo Locations
}
