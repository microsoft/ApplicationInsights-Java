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

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.*;

public class AttachStatsbeat extends BaseStatsbeat {

    private String resourceProviderId;
    private MetadataInstanceResponse metadataInstanceResponse;

    public AttachStatsbeat(TelemetryClient telemetryClient, long interval) {
        super(telemetryClient, interval);
        resourceProviderId = initResourceProviderId(CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_RP));
        AzureMetadataService.getInstance().initialize(interval);
    }

    @Override
    protected void send() {
        MetricTelemetry statsbeatTelemetry = createStatsbeatTelemetry(ATTACH, 0);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_RP_ID, resourceProviderId);
        telemetryClient.track(statsbeatTelemetry);
    }

    /**
     * @return the unique identifier of the resource provider.
     */
    public String getResourceProviderId() {
        return resourceProviderId;
    }

    public void setResourceProviderId(String resourceProviderId) {
        this.resourceProviderId = resourceProviderId;
    }

    public MetadataInstanceResponse getMetadataInstanceResponse() {
        return metadataInstanceResponse;
    }

    public void updateMetadataInstance(MetadataInstanceResponse response) {
        if (response != null) {
            metadataInstanceResponse = response;
            resourceProviderId = initResourceProviderId(RP_VM);
        }
    }

    protected String initResourceProviderId(String resourceProvider) {
        switch (resourceProvider) {
            case RP_APPSVC:
                return getEnvironmentVariable(WEBSITE_SITE_NAME) + "/" + getEnvironmentVariable(WEBSITE_HOME_STAMPNAME) + "/" + getEnvironmentVariable(WEBSITE_HOSTNAME);
            case RP_FUNCTIONS:
                return getEnvironmentVariable(WEBSITE_HOSTNAME);
            case RP_VM:
                if (metadataInstanceResponse != null) {
                    return metadataInstanceResponse.getVmId() + "/" + metadataInstanceResponse.getSubscriptionId();
                } else {
                    return UNKNOWN;
                }
            case RP_AKS: // TODO will update resourceProviderId when cluster_id becomes available from the AKS AzureMetadataService extension.
            case UNKNOWN:
            default:
                return UNKNOWN;
        }
    }

    @VisibleForTesting
    String getEnvironmentVariable(String envVar) {
        return System.getenv(envVar);
    }
}
