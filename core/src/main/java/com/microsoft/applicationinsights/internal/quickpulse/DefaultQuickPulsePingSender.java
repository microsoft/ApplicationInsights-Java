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

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import com.microsoft.applicationinsights.internal.channel.common.LazyHttpClient;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gupele on 12/12/2016.
 */
final class DefaultQuickPulsePingSender implements QuickPulsePingSender {

    private static final Logger logger = LoggerFactory.getLogger(DefaultQuickPulsePingSender.class);

    private static final String QP_BASE_URI = "https://rt.services.visualstudio.com/QuickPulseService.svc";

    private final TelemetryConfiguration configuration;
    private final HttpClient httpClient;
    private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
    private String pingPrefix;
    private String roleName;
    private final String instanceName;
    private final String machineName;
    private final String quickPulseId;
    private long lastValidTransmission = 0;
    private static final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();

    public DefaultQuickPulsePingSender(HttpClient httpClient, TelemetryConfiguration configuration, String machineName, String instanceName, String roleName, String quickPulseId) {
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.roleName = roleName;
        this.instanceName = instanceName;
        this.machineName = machineName;
        this.quickPulseId = quickPulseId;

        if (!LocalStringsUtils.isNullOrEmpty(roleName)) {
            roleName = "\"" + roleName + "\"";
        }

        updatePingPrefix();

        if (logger.isTraceEnabled()) {
            logger.trace("{} using endpoint {}", DefaultQuickPulsePingSender.class.getSimpleName(), getQuickPulseEndpoint());
        }
    }

    /**
     * @deprecated Use {@link #DefaultQuickPulsePingSender(HttpClient, TelemetryConfiguration, String, String, String, String)}
     */
    @Deprecated
    public DefaultQuickPulsePingSender(final HttpClient httpClient, final String machineName, final String instanceName, final String roleName, final String quickPulseId) {
        this(httpClient, null, machineName, instanceName, roleName, quickPulseId);
    }

    @Override
    public QuickPulseHeaderInfo ping(String redirectedEndpoint) {
        final Date currentDate = new Date();
        final String endpointPrefix = LocalStringsUtils.isNullOrEmpty(redirectedEndpoint) ? getQuickPulseEndpoint() : redirectedEndpoint;
        String pingUrl = getQuickPulsePingUri(endpointPrefix);
        if (Strings.isNullOrEmpty(pingUrl)) {
            // Quick Pulse Ping uri will be null when the instrumentation key is null. When that happens, turn off quick pulse.
            return new QuickPulseHeaderInfo(QuickPulseStatus.QP_IS_OFF);
        }

        // Linux Consumption Plan role name is lazily set. Need to update pingPrefix accordingly.
        if (Strings.isNullOrEmpty(roleName)) {
            roleName = TelemetryConfiguration.getActive().getRoleName();
            updatePingPrefix();
        }
        final HttpPost request = networkHelper.buildPingRequest(currentDate, pingUrl, quickPulseId, machineName, roleName, instanceName);

        final ByteArrayEntity pingEntity = buildPingEntity(currentDate.getTime());
        request.setEntity(pingEntity);

        final long sendTime = System.nanoTime();
        HttpResponse response = null;
        try {
            response = httpClient.execute(request);
            if (networkHelper.isSuccess(response)) {
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
        } catch (IOException e) {
            // chomp
        } finally {
            if (response != null) {
                LazyHttpClient.dispose(response);
            }
        }
        return onPingError(sendTime);
    }

    private void updatePingPrefix() {
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
    }

    @VisibleForTesting
    String getQuickPulsePingUri(String endpointPrefix) {
        String instrumentationKey = getInstrumentationKey();
        if (Strings.isNullOrEmpty(instrumentationKey)) {
            return null;
        }

        return endpointPrefix + "/ping?ikey=" + getInstrumentationKey();
    }

    private String getInstrumentationKey() {
        TelemetryConfiguration config = this.configuration == null ? TelemetryConfiguration.getActive() : configuration;
        return config.getInstrumentationKey();
    }

    @VisibleForTesting
    String getQuickPulseEndpoint() {
        if (configuration != null) {
            return configuration.getEndpointProvider().getLiveEndpointURL().toString();
        } else {
            return QP_BASE_URI;
        }
    }

    private ByteArrayEntity buildPingEntity(long timeInMillis) {
        String sb = pingPrefix + timeInMillis +
                ")\\/\"," +
                "\"Version\":\"2.2.0-738\"" +
                "}";
        return new ByteArrayEntity(sb.getBytes());
    }

    private QuickPulseHeaderInfo onPingError(long sendTime) {
        final double timeFromLastValidTransmission = (sendTime - lastValidTransmission) / 1000000000.0;
        if (timeFromLastValidTransmission >= 60.0) {
            return new QuickPulseHeaderInfo(QuickPulseStatus.ERROR);
        }

        return new QuickPulseHeaderInfo(QuickPulseStatus.QP_IS_OFF);
    }
}
