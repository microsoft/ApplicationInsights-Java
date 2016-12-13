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

package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.internal.channel.common.ApacheSender;
import com.microsoft.applicationinsights.internal.util.DeviceInfo;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Created by gupele on 12/12/2016.
 */
public final class QuickPulsePingSender {
    private final static String QP_BASE_URI = "https://rt.services.visualstudio.com/QuickPulseService.svc/ping?ikey=";
    private final static String QP_STATUS_HEADER = "x-ms-qps-subscribed";
    private static final String HEADER_TRANSMISSION_TIME = "x-ms-qps-transmission-time";
    private final static int SECONDS_IN_HOUR = 60 * 60;
    private final static long TICKS_AT_EPOCH = 621355968000000000L;

    private final String quickPulsePingUri;
    private final ApacheSender apacheSender;
    private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
    private String pingPrefix;
    private long lastValidTransmission = 0;

    public QuickPulsePingSender(final ApacheSender apacheSender, final String instanceName, final String quickPulseId) {
        this.apacheSender = apacheSender;

        final String ikey = TelemetryConfiguration.getActive().getInstrumentationKey();
        quickPulsePingUri = QP_BASE_URI + "ping?ikey=" + ikey;

        final StrBuilder sb = new StrBuilder();
        sb.append("\"Instance\":\"" + instanceName + "\"," + "\"InstrumentationKey\":");
        sb.append(ikey);
        sb.append(",\"InvariantVersion\":2,\"MachineName\":\"");
        sb.append(instanceName);
        sb.append("\"");
        sb.append(",\"Version\":\"2.2.0-424\"");
        sb.append(",\"StreamId\":");
        sb.append(quickPulseId);

        sb.append(",\"Documents\":null");
        sb.append(",\"Metrics\":null");
        sb.append(",\"Timestamp\": \"\\/Date(");

        pingPrefix = sb.toString();
    }

    public QuickPulseNetworkHelper.QuickPulseStatus ping() {
        final Date currentDate = new Date();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        HttpPost request = buildRequest(calendar, quickPulsePingUri);

        ByteArrayEntity pingEntity = buildPingEntity(calendar.getTimeInMillis());
        request.setEntity(pingEntity);

        final long sendTime = System.nanoTime();
        try {
            HttpResponse response = apacheSender.sendPostRequest(request);
            if (networkHelper.isSuccess(response)) {
                final QuickPulseNetworkHelper.QuickPulseStatus quickPulseResultStatus = networkHelper.getQuickPulseStatus(response);
                switch (quickPulseResultStatus) {
                    case QP_IS_OFF:
                    case QP_IS_ON:
                        lastValidTransmission = sendTime;
                        return quickPulseResultStatus;

                    case ERROR:
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException e) {
        }
        return onPingError(sendTime);
    }

    private ByteArrayEntity buildPingEntity(long timeInMillis) {
        long ms = System.currentTimeMillis();

        StrBuilder sb = new StrBuilder(pingPrefix);
        sb.append(timeInMillis);
        sb.append(")\\\\/\\\"\"");
        ByteArrayEntity bae = new ByteArrayEntity(sb.toString().getBytes());
        return bae;
    }

    private HttpPost buildRequest(Calendar currentDate, String address) {
        long ticks = currentDate.get(Calendar.SECOND);
        ticks += currentDate.get(Calendar.MINUTE) * 60;
        ticks += currentDate.get(Calendar.HOUR) * SECONDS_IN_HOUR;
        ticks = ticks * 1000 * 10000;
        ticks += TICKS_AT_EPOCH;

        HttpPost request = new HttpPost(address);
        request.addHeader(HEADER_TRANSMISSION_TIME, String.valueOf(ticks));
        return request;
    }

    private QuickPulseNetworkHelper.QuickPulseStatus onPingError(long sendTime) {
        final double timeFromLastValidTransmission = (sendTime - lastValidTransmission) / 1000000000.0;
        if (timeFromLastValidTransmission >= 60.0) {
            return QuickPulseNetworkHelper.QuickPulseStatus.ERROR;
        }

        return QuickPulseNetworkHelper.QuickPulseStatus.QP_IS_OFF;
    }
}
