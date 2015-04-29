package com.microsoft.applicationinsights.management.rest;

import java.io.IOException;
import java.util.List;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.management.rest.model.ResourceGroup;
import com.microsoft.applicationinsights.management.rest.model.Subscription;
import com.microsoft.applicationinsights.management.rest.operations.*;

/**
 * Created by yonisha on 4/21/2015.
 */
public interface ManagementClient {

    // region Subscriptions

    /**
     * Gets a list of available subscriptions.
     * @return The list of subscriptions available.
     */
    List<Subscription> getSubscriptions() throws IOException, AzureCmdException;

    // endregion Subscriptions

    // region Resources

    /**
     * Gets a list of resources for a given subscription.
     * @param subscriptionId The subscription ID.
     * @return The resources list.
     */
    List<Resource> getResources(String subscriptionId) throws IOException, AzureCmdException;

    /**
     * Creates a new resource.
     * @param subscriptionId The subscription which the resource will be created in.
     * @param resourceGroupName The resource group name.
     * @param resourceName The resource name.
     * @return The resource created.
     */
    Resource createResource(String subscriptionId, String resourceGroupName, String resourceName) throws IOException, AzureCmdException;

    // endregion Resources

    // region Resource Groups

    /**
     * Creates new resource group.
     * @param subscriptionId The subsription ID.
     * @param resourceGroupName The resource group name.
     * @param location The location.
     * @return The new resource group created.
     */
    ResourceGroup createResourceGroup(String subscriptionId, String resourceGroupName, String location) throws IOException, AzureCmdException;

    /**
     * Gets all resource groups in the given subscription.
     * @param subscriptionId The subscription ID.
     * @return Collection of resource groups.
     */
    List<ResourceGroup> getResourceGroups(String subscriptionId) throws IOException, AzureCmdException;

    // endregion Resource Groups

    // region Geo Locations

    /**
     * Gets all the available geo-locations.
     * @return Collection of available geo-locations.
     */
    List<String> getAvailableGeoLocations() throws IOException, AzureCmdException;

    // endregion Geo Locations
}
