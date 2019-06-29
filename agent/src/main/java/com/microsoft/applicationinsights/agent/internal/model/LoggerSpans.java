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

package com.microsoft.applicationinsights.agent.internal.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge.ExceptionTelemetry;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge.TraceTelemetry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.MessageSupplier;
import org.glowroot.instrumentation.api.internal.ReadableMessage;

class LoggerSpans {

    static void track(SdkBridge sdkBridge, MessageSupplier messageSupplier, @Nullable Throwable throwable,
                      long timeMillis) {

        ReadableMessage message = (ReadableMessage) messageSupplier.get();
        String formattedMessage = message.getText();
        Map<String, ?> detail = message.getDetail();
        String level = (String) detail.get("Level");
        String loggerName = (String) detail.get("Logger name");

        if (throwable == null) {
            TraceTelemetry telemetry = new TraceTelemetry(formattedMessage, level);
            setProperties(telemetry.getProperties(), timeMillis, level, loggerName, null, formattedMessage);
            sdkBridge.track(telemetry);
        } else {
            ExceptionTelemetry telemetry = new ExceptionTelemetry(throwable, level);
            setProperties(telemetry.getProperties(), timeMillis, level, loggerName, throwable, formattedMessage);
            sdkBridge.track(telemetry);
        }
    }

    private static void setProperties(Map<String, String> properties, long timeMillis, String level, String loggerName,
                                      @Nullable Throwable throwable, String formattedMessage) {

        // TODO SourceType? e.g. "Log4j", "LOGBack"
        properties.put("SourceType", "Logger");
        properties.put("TimeStamp", getFormattedDate(timeMillis));
        if (level != null) {
            properties.put("LoggingLevel", level);
        }
        if (loggerName != null) {
            properties.put("LoggerName", loggerName);
        }
        // TODO ThreadName

        if (throwable != null) {
            properties.put("Logger Message", formattedMessage);
        }

        // TODO log4j: location information? ClassName, FileName, MethodName, LineNumber

        // TODO log4j and logback custom log event properties?

        // TODO: Username, domain and identity should be included as in .NET version.
        // TODO: Should check, seems that it is not included in Log4j2.
    }

    private static String getFormattedDate(long dateInMilliseconds) {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).format(new Date(dateInMilliseconds));
    }
}
