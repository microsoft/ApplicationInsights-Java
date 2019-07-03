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

package com.microsoft.applicationinsights.agentc.internal.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.MessageSupplier;
import org.glowroot.instrumentation.api.internal.ReadableMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LoggerSpans {

    private static final Logger logger = LoggerFactory.getLogger(LoggerSpans.class);

    public static void track(String operationId, String operationParentId, MessageSupplier messageSupplier,
                             @Nullable Throwable throwable, long timeMillis) {

        ReadableMessage message = (ReadableMessage) messageSupplier.get();
        String formattedMessage = message.getText();
        Map<String, ?> detail = message.getDetail();
        String level = (String) detail.get("Level");
        SeverityLevel severityLevel = level == null ? null : toSeverityLevel(level);

        String loggerName = (String) detail.get("Logger name");

        if (throwable == null) {
            TraceTelemetry telemetry = new TraceTelemetry(formattedMessage);
            telemetry.getContext().getOperation().setId(operationId);
            telemetry.getContext().getOperation().setParentId(operationParentId);
            if (severityLevel != null) {
                telemetry.setSeverityLevel(severityLevel);
            }
            setProperties(telemetry.getProperties(), timeMillis, level, loggerName, null, formattedMessage);
            Global.getTelemetryClient().track(telemetry);
        } else {
            ExceptionTelemetry telemetry = new ExceptionTelemetry(throwable);
            telemetry.getContext().getOperation().setId(operationId);
            telemetry.getContext().getOperation().setParentId(operationParentId);
            if (severityLevel != null) {
                telemetry.setSeverityLevel(severityLevel);
            }
            setProperties(telemetry.getProperties(), timeMillis, level, loggerName, throwable, formattedMessage);
            Global.getTelemetryClient().track(telemetry);
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

    private static SeverityLevel toSeverityLevel(String level) {
        switch (level) {
            case "FATAL":
                return SeverityLevel.Critical;
            case "ERROR":
                return SeverityLevel.Error;
            case "WARN":
                return SeverityLevel.Warning;
            case "INFO":
                return SeverityLevel.Information;
            case "DEBUG":
            case "TRACE":
            case "ALL":
                return SeverityLevel.Verbose;
            default:
                logger.error("Unexpected level {}, using TRACE level as default", level);
                return SeverityLevel.Verbose;
        }
    }
}
