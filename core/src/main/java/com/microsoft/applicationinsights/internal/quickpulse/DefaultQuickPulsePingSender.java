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

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.channel.common.ApacheSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gupele on 12/12/2016.
 */
final class DefaultQuickPulsePingSender implements QuickPulsePingSender {

    private static final Logger logger = LoggerFactory.getLogger(DefaultQuickPulsePingSender.class);

    private static final String QP_BASE_URI = "https://rt.services.visualstudio.com/QuickPulseService.svc";

    private final TelemetryConfiguration configuration;
    private final ApacheSender apacheSender;
    private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
    private String pingPrefix;
    private long lastValidTransmission = 0;

    public DefaultQuickPulsePingSender(ApacheSender sender, TelemetryConfiguration configuration, String instanceName, String quickPulseId) {
        this.configuration = configuration;
        this.apacheSender = sender;

        pingPrefix = "{" +
                "\"Documents\": null," +
                "\"Instance\":\"" + instanceName + "\"," +
                "\"InstrumentationKey\": null," +
                "\"InvariantVersion\": 2," +
                "\"MachineName\":\"" + instanceName + "\"," +
                "\"Metrics\": null," +
                "\"StreamId\": \"" + quickPulseId + "\"," +
                "\"Timestamp\": \"\\/Date(";
        if (logger.isTraceEnabled()) {
            logger.trace("{} using endpoint {}", DefaultQuickPulsePingSender.class.getSimpleName(), getQuickPulseEndpoint());
        }
    }

    /**
     * @deprecated Use {@link #DefaultQuickPulsePingSender(ApacheSender, TelemetryConfiguration, String, String)}
     */
    @Deprecated
    public DefaultQuickPulsePingSender(final ApacheSender apacheSender, final String instanceName, final String quickPulseId) {
        this(apacheSender, null, instanceName, quickPulseId);
    }

    @Override
    public QuickPulseStatus ping() {
        final Date currentDate = new Date();
        final HttpPost request = networkHelper.buildRequest(currentDate, getQuickPulsePingUri());

        final ByteArrayEntity pingEntity = buildPingEntity(currentDate.getTime());
        request.setEntity(pingEntity);

        final long sendTime = System.nanoTime();
        HttpResponse response = null;
        try {
            response = apacheSender.sendPostRequest(request);
            if (networkHelper.isSuccess(response)) {
                final QuickPulseStatus quickPulseResultStatus = networkHelper.getQuickPulseStatus(response);
                switch (quickPulseResultStatus) {
                    case QP_IS_OFF:
                    case QP_IS_ON:
                        lastValidTransmission = sendTime;
                        return quickPulseResultStatus;

                    default:
                        break;
                }
            }
        } catch (IOException e) {
            // chomp
        } finally {
            if (response != null) {
                apacheSender.dispose(response);
            }
        }
        return onPingError(sendTime);
    }

    @VisibleForTesting
    String getQuickPulsePingUri() {
        return getQuickPulseEndpoint() + "/ping?ikey=" + getInstrumentationKey();
    }

    private String getInstrumentationKey() {
        TelemetryConfiguration config = this.configuration == null ? TelemetryConfiguration.getActive() : configuration;
        return config.getInstrumentationKey();
    }

    private String getQuickPulseEndpoint() {
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

    private QuickPulseStatus onPingError(long sendTime) {
        final double timeFromLastValidTransmission = (sendTime - lastValidTransmission) / 1000000000.0;
        if (timeFromLastValidTransmission >= 60.0) {
            return QuickPulseStatus.ERROR;
        }

        return QuickPulseStatus.QP_IS_OFF;
    }
}
