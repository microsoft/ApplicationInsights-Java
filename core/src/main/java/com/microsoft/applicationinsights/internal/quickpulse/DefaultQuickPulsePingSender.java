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

package com.microsoft.applicationinsights.internal.quickpulse;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.azure.core.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gupele on 12/12/2016.
 */
final class DefaultQuickPulsePingSender implements QuickPulsePingSender {

    private static final Logger logger = LoggerFactory.getLogger(DefaultQuickPulsePingSender.class);

    private final TelemetryClient telemetryClient;
    private final HttpPipeline httpPipeline;
    private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
    private final String pingPrefix;
    private final String roleName;
    private final String instanceName;
    private final String machineName;
    private final String quickPulseId;
    private long lastValidTransmission = 0;
    private static final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();

    public DefaultQuickPulsePingSender(HttpPipeline httpPipeline, TelemetryClient telemetryClient, String machineName, String instanceName, String roleName, String quickPulseId) {
        this.telemetryClient = telemetryClient;
        this.httpPipeline = httpPipeline;
        this.roleName = roleName;
        this.instanceName = instanceName;
        this.machineName = machineName;
        this.quickPulseId = quickPulseId;

        if (!LocalStringsUtils.isNullOrEmpty(roleName)) {
            roleName = "\"" + roleName + "\"";
        }

        pingPrefix = "{" +
                "\"Documents\": null," +
                "\"Instance\":\"" + instanceName + "\"," +
                "\"InstrumentationKey\": null," +
                "\"InvariantVersion\": " + QuickPulse.QP_INVARIANT_VERSION + "," +
                "\"MachineName\":\"" + machineName + "\"," +
                "\"RoleName\":" + roleName + "," +
                "\"Metrics\": null," +
                "\"StreamId\": \"" + quickPulseId + "\"," +
                "\"Timestamp\": \"\\/Date(";

        if (logger.isTraceEnabled()) {
            logger.trace("{} using endpoint {}", DefaultQuickPulsePingSender.class.getSimpleName(), getQuickPulseEndpoint());
        }
    }

    @Override
    public QuickPulseHeaderInfo ping(String redirectedEndpoint) {
        final Date currentDate = new Date();
        final String endpointPrefix = LocalStringsUtils.isNullOrEmpty(redirectedEndpoint) ? getQuickPulseEndpoint() : redirectedEndpoint;
        final HttpRequest request = networkHelper.buildPingRequest(currentDate, getQuickPulsePingUri(endpointPrefix), quickPulseId, machineName, roleName, instanceName);
        request.setBody(buildPingEntity(currentDate.getTime()));

        final long sendTime = System.nanoTime();
        HttpResponse response = null;
        try {

            response = httpPipeline.send(request).block();
            if (response != null && networkHelper.isSuccess(response)) {
                final QuickPulseHeaderInfo quickPulseHeaderInfo = networkHelper.getQuickPulseHeaderInfo(response);
                switch (quickPulseHeaderInfo.getQuickPulseStatus()) {
                    case QP_IS_OFF:
                    case QP_IS_ON:
                        lastValidTransmission = sendTime;
                        return quickPulseHeaderInfo;

                    default:
                        break;
                }
            }
        } catch (FriendlyException e) {
            if(!friendlyExceptionThrown.getAndSet(true)) {
                logger.error(e.getMessage());
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return onPingError(sendTime);
    }

    // visible for testing
    String getQuickPulsePingUri(String endpointPrefix) {
        return endpointPrefix + "/ping?ikey=" + getInstrumentationKey();
    }

    private String getInstrumentationKey() {
        return telemetryClient.getInstrumentationKey();
    }

    // visible for testing
    String getQuickPulseEndpoint() {
        return telemetryClient.getEndpointProvider().getLiveEndpointUrl().toString();
    }

    private String buildPingEntity(long timeInMillis) {
         return pingPrefix + timeInMillis +
                ")\\/\"," +
                "\"Version\":\"2.2.0-738\"" +
                "}";
    }

    private QuickPulseHeaderInfo onPingError(long sendTime) {
        final double timeFromLastValidTransmission = (sendTime - lastValidTransmission) / 1000000000.0;
        if (timeFromLastValidTransmission >= 60.0) {
            return new QuickPulseHeaderInfo(QuickPulseStatus.ERROR);
        }

        return new QuickPulseHeaderInfo(QuickPulseStatus.QP_IS_OFF);
    }
}
