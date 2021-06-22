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

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryUtil;

class AttachStatsbeat extends BaseStatsbeat {

    private static final String ATTACH_METRIC_NAME = "Attach";

    private static final String UNKNOWN_RP_ID = "unknown";

    private static final String WEBSITE_SITE_NAME = "appSrv_SiteName";
    private static final String WEBSITE_HOSTNAME = "appSrv_wsHost";
    private static final String WEBSITE_HOME_STAMPNAME = "appSrv_wsStamp";

    private volatile String resourceProviderId;
    private volatile MetadataInstanceResponse metadataInstanceResponse;

    AttachStatsbeat(TelemetryClient telemetryClient, long interval) {
        super(telemetryClient, interval);
        resourceProviderId = initResourceProviderId(CustomDimensions.get().getResourceProvider(), null);
    }

    @Override
    protected void send() {
        TelemetryItem statsbeatTelemetry = createStatsbeatTelemetry(ATTACH_METRIC_NAME, 0);
        TelemetryUtil.getProperties(statsbeatTelemetry.getData().getBaseData())
                .put("rpId", resourceProviderId);
        telemetryClient.trackAsync(statsbeatTelemetry);
    }

    /**
     * @return the unique identifier of the resource provider.
     */
    String getResourceProviderId() {
        return resourceProviderId;
    }

    MetadataInstanceResponse getMetadataInstanceResponse() {
        return metadataInstanceResponse;
    }

    void updateMetadataInstance(MetadataInstanceResponse response) {
        metadataInstanceResponse = response;
        resourceProviderId = initResourceProviderId(ResourceProvider.RP_VM, response);
    }

    // visible for testing
    static String initResourceProviderId(ResourceProvider resourceProvider, MetadataInstanceResponse response) {
        switch (resourceProvider) {
            case RP_APPSVC:
                return System.getenv(WEBSITE_SITE_NAME) + "/" + System.getenv(WEBSITE_HOME_STAMPNAME) + "/" + System.getenv(WEBSITE_HOSTNAME);
            case RP_FUNCTIONS:
                return System.getenv(WEBSITE_HOSTNAME);
            case RP_VM:
                if (response != null) {
                    return response.getVmId() + "/" + response.getSubscriptionId();
                } else {
                    return UNKNOWN_RP_ID;
                }
            case RP_AKS: // TODO will update resourceProviderId when cluster_id becomes available from the AKS AzureMetadataService extension.
            case UNKNOWN:
                return UNKNOWN_RP_ID;
        }
        return UNKNOWN_RP_ID;
    }
}
