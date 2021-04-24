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

package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.*;

public class AttachStatsbeat extends BaseStatsbeat {

    private String resourceProviderId;
    private MetadataInstanceResponse metadataInstanceResponse;

    public AttachStatsbeat() {
        super();
        initResourceProviderId();
    }

    @Override
    protected void send(TelemetryClient telemetryClient) {
        StatsbeatTelemetry statsbeatTelemetry = createStatsbeatTelemetry(ATTACH, 0);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_RP_ID, resourceProviderId);
        telemetryClient.track(statsbeatTelemetry);
    }

    @Override
    protected void reset() {
        resourceProviderId = null;
        metadataInstanceResponse = null;
    }

    /**
     * @return the unique identifier of the resource provider.
     */
    public String getResourceProviderId() {
        return resourceProviderId;
    }

    public MetadataInstanceResponse getMetadataInstanceResponse() {
        return metadataInstanceResponse;
    }

    public void updateMetadataInstance(MetadataInstanceResponse response) {
        if (response != null) {
            metadataInstanceResponse = response;
            updateResourceProvider(RP_VM);
            updateOperatingSystem();
        }
    }

    protected void updateResourceProvider(String rp) {
        resourceProvider = rp;
        initResourceProviderId();
    }

    // osType from the Azure Metadata Service has a higher precedence over the running app’s operating system.
    private void updateOperatingSystem() {
        String osType = metadataInstanceResponse.getOsType();
        if (osType != null && !"unknown".equalsIgnoreCase(osType)) {
            operatingSystem = osType;
        }
    }

    private void initResourceProviderId() {
        switch (resourceProvider) {
            case RP_APPSVC:
                resourceProviderId = String.format("%s/%s/%s", System.getenv().get(WEBSITE_SITE_NAME), System.getenv().get(WEBSITE_HOME_STAMPNAME), System.getenv().get(WEBSITE_HOSTNAME));
                break;
            case RP_FUNCTIONS:
                resourceProviderId = System.getenv().get(WEBSITE_HOSTNAME);
                break;
            case RP_VM:
                resourceProviderId = String.format("%s/%s", metadataInstanceResponse.getVmId(), metadataInstanceResponse.getSubscriptionId());
                break;
            case RP_AKS: // TODO will update resourceProviderId when cluster_id becomes available from the AKS AzureMetadataService extension.
            case UNKNOWN:
            default:
                resourceProviderId = UNKNOWN;
                break;
        }
    }
}
