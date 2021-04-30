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
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.*;

public class AttachStatsbeat extends BaseStatsbeat {

    private static final Logger logger = LoggerFactory.getLogger(AttachStatsbeat.class);
    private String resourceProviderId;
    private MetadataInstanceResponse metadataInstanceResponse;

    public AttachStatsbeat(TelemetryClient telemetryClient, long interval) {
        super(telemetryClient, interval);
        initResourceProviderId();
        AzureMetadataService.getInstance().initialize();
    }

    @Override
    protected void send() {
        if (resourceProviderId != null) {
            MetricTelemetry statsbeatTelemetry = createStatsbeatTelemetry(ATTACH, 0);
            statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_RP_ID, resourceProviderId);
            telemetryClient.track(statsbeatTelemetry);
            logger.debug("send a AttachStatsbeat {}", statsbeatTelemetry);
        }
    }

    @Override
    protected synchronized void reset() {
        resourceProviderId = null;
        metadataInstanceResponse = null;
        initResourceProviderId();
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
        commonProperties.resourceProvider = rp;
        initResourceProviderId();
    }

    // osType from the Azure Metadata Service has a higher precedence over the running app’s operating system.
    private void updateOperatingSystem() {
        String osType = metadataInstanceResponse.getOsType();
        if (osType != null && !"unknown".equalsIgnoreCase(osType)) {
            commonProperties.operatingSystem = osType;
        }
    }

    private void initResourceProviderId() {
        switch (commonProperties.resourceProvider) {
            case RP_APPSVC:
                resourceProviderId = System.getenv().get(WEBSITE_SITE_NAME) + "/" + System.getenv().get(WEBSITE_HOME_STAMPNAME) + "/" + System.getenv().get(WEBSITE_HOSTNAME);
                break;
            case RP_FUNCTIONS:
                resourceProviderId = System.getenv().get(WEBSITE_HOSTNAME);
                break;
            case RP_VM:
                resourceProviderId = metadataInstanceResponse.getVmId() + "/" + metadataInstanceResponse.getSubscriptionId();
                break;
            case RP_AKS: // TODO will update resourceProviderId when cluster_id becomes available from the AKS AzureMetadataService extension.
            case UNKNOWN:
            default:
                resourceProviderId = UNKNOWN;
                break;
        }
    }
}
