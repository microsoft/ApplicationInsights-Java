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
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

import java.util.concurrent.Executors;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.*;

public class AttachStatsbeat extends BaseStatsbeat {

    private static volatile String resourceProviderId;
    private static volatile MetadataInstanceResponse metadataInstanceResponse;

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
            case RP_AKS:
            case RP_UNKNOWN:
            default:
                resourceProviderId = RP_UNKNOWN;
                break;
        }
    }

    /**
     * @return the unique identifier of the resource provider.
     */
    public String getResourceProviderId() {
        return resourceProviderId;
    }

    public static MetadataInstanceResponse getMetadataInstanceResponse() {
        return metadataInstanceResponse;
    }

    public static void updateMetadataInstance(MetadataInstanceResponse response) {
        if (response != null) {
            metadataInstanceResponse = response;
            updateOperatingSystem();
            resourceProviderId = String.format("%s/%s", metadataInstanceResponse.getVmId(), metadataInstanceResponse.getSubscriptionId());
        }
    }

    // osType from the Azure Metadata Service has a higher precedence over the running app’s operating system.
    private static void updateOperatingSystem() {
        String osType = metadataInstanceResponse.getOsType();
        if (osType != null && !"unknown".equalsIgnoreCase(osType)) {
            operatingSystem = osType;
        }
    }
}
