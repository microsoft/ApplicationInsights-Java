import com.microsoft.applicationinsights.management.authentication.Authenticator;
import com.microsoft.applicationinsights.management.rest.ApplicationInsightsManagementClient;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.management.rest.model.ResourceGroup;
import com.microsoft.applicationinsights.management.rest.model.Subscription;
import com.microsoft.applicationinsights.management.rest.operations.AzureCmdException;
import com.microsoftopentechnologies.aad.adal4j.AuthenticationResult;

import java.io.IOException;
import java.util.List;

/**
 * Created by yonisha on 4/19/2015.
 */
public class Program {

    private static ApplicationInsightsManagementClient client;

    public static void main(String[] args) throws Exception {

        String userAgent = String.format(
                "%s/%s (lang=%s; os=%s; version=%s)",
                "SomeID",
                "10.0",
                "Java",
                System.getProperty("os.name"),
                "10.0");

        AuthenticationResult result = Authenticator.getAuthenticationResult();
        client = new ApplicationInsightsManagementClient(result, userAgent);

        if (result == null) {
            System.out.println("Authentication canceled..");
            System.exit(1);
        }

        String requiredSubscriptionID = "a866e082-246e-4d8b-89df-a9191c5f1aca";
        String resourceGroup = "GroupNE";
        String appName = "yonisha-new-app3";
        String location = "Central US";

//        getSubscriptions();
//        getResources(requiredSubscriptionID);
        getAvailableGeoLocations();
//        getResourceGroups(requiredSubscriptionID);

//        createResourceGroup(requiredSubscriptionID, resourceGroup, location);
//        createResource(requiredSubscriptionID, resourceGroup, appName);

        System.exit(0);
    }

    private static List<ResourceGroup> getResourceGroups(String subId) throws IOException, AzureCmdException {
        List<ResourceGroup> resourceGroups = client.getResourceGroups(subId);

        for (ResourceGroup rg : resourceGroups) {
            System.out.println(rg.getId());
            System.out.println(rg.getName());
            System.out.println(rg.getLocation());
        }

        return resourceGroups;
    }

    private static ResourceGroup createResourceGroup(String subId, String resourceGroupName, String location) throws IOException, AzureCmdException {
        System.out.println("Creating resource group: " + resourceGroupName);
        ResourceGroup resourceGroup = client.createResourceGroup(subId, resourceGroupName, location);

        System.out.println(resourceGroup.getName());
        System.out.println(resourceGroup.getId());
        System.out.println(resourceGroup.getLocation());

        return resourceGroup;
    }

    private static void getResources(String requiredSubscriptionID) throws IOException, AzureCmdException {
        System.out.println("Getting resources");
        List<Resource> resources = client.getResources(requiredSubscriptionID);

        for (Resource component : resources) {
            System.out.println("Component:\t\t" + component.getName());
            System.out.println("\t" + component.getLocation());
            System.out.println("\t" + component.getId());
            System.out.println("\t" + component.getType());

            if (component.getTags() != null) {
                System.out.println("\tTags");
                for (String tag : component.getTags()) {
                    System.out.println("\t\t" + tag);
                }
            }
        }
    }

    private static void getSubscriptions() throws IOException, AzureCmdException {
        System.out.println("Getting subscription");
        List<Subscription> subscriptions = client.getSubscriptions();
        for (Subscription sub : subscriptions) {
            System.out.println(sub.getName());
            System.out.println(sub.getId());
        }

    }
    private static void getAvailableGeoLocations() throws IOException, AzureCmdException {
        List<String> availableGeoLocations = client.getAvailableGeoLocations();

        for (String location : availableGeoLocations) {
            System.out.println(location);
        }
    }

    private static void createResource(String requiredSubscriptionID, String appName, String resourceGroup) throws IOException, AzureCmdException {
        Resource resource = client.createResource(requiredSubscriptionID, appName, resourceGroup);

        if (resource == null) {
            System.out.println("NULL RESOURCE");
            System.exit(3);
        }

        System.out.println(resource.getId());
        System.out.println(resource.getName());
        System.out.println(resource.getLocation());
        System.out.println(resource.getType());
    }
}
