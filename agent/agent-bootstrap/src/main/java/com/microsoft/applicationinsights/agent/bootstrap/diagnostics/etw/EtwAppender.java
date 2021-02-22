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
package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw;

import java.util.Map;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.ApplicationMetadataFactory;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaError;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaInfo;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaVerbose;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.IpaWarn;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.status.StatusFile;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

public class EtwAppender extends AppenderBase<ILoggingEvent> {
    private final EtwProvider etwProvider;
    private final IpaEtwEventBase proto;

    public EtwAppender() {
        ApplicationMetadataFactory metadata = DiagnosticsHelper.getMetadataFactory();

        proto = new IpaInfo();
        proto.setAppName(metadata.getSiteName().getValue());
        proto.setExtensionVersion(metadata.getSdkVersion().getValue());
        proto.setSubscriptionId(metadata.getSubscriptionId().getValue());

        etwProvider = new EtwProvider(metadata.getSdkVersion().getValue());
    }

    @Override
    public void start() {
        IpaVerbose event = new IpaVerbose(proto);
        event.setMessageFormat("EtwProvider initialized sucessfully.");
        try {
            this.etwProvider.writeEvent(event);
        } catch (LinkageError | ApplicationInsightsEtwException e) {
            final String message = "EtwProvider failed to initialize.";
            LoggerFactory.getLogger(DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME).error(message, e);
            addError(message, e);

            StatusFile.putValue("EtwProviderInitialized", "false");
            StatusFile.putValue("EtwProviderError", e.getLocalizedMessage());
            StatusFile.write();

            return; // appender fails to start
        }

        StatusFile.putValueAndWrite("EtwProviderInitialized", "true");
        super.start();
    }

    @Override
    protected void append(ILoggingEvent logEvent) {
        String logger = logEvent.getLoggerName();
        if (logger != null && logger.startsWith("com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.")) {
            addWarn("Skipping attempt to log to "+logger);
            return;
        }

        Level level = logEvent.getLevel();
        IpaEtwEventBase event;
        // empty if no throwable
        switch (level.levelInt) {
            case Level.ERROR_INT:
                IpaError error = new IpaError(proto);
                error.setStacktrace(ThrowableProxyUtil.asString(logEvent.getThrowableProxy()));
                event = error;
                break;
            case Level.WARN_INT:
                IpaWarn warn = new IpaWarn(proto);
                warn.setStacktrace(ThrowableProxyUtil.asString(logEvent.getThrowableProxy()));
                event = warn;
                break;
            case Level.INFO_INT:
                event = new IpaInfo(proto);
                break;
            default:
                addWarn("Unsupported log level: " + level.levelStr);
                return;
        }

        Map<String, String> mdcPropertyMap = logEvent.getMDCPropertyMap();
        if (!mdcPropertyMap.isEmpty()) {
            String operation = mdcPropertyMap.get(DiagnosticsHelper.MDC_PROP_OPERATION);
            if (operation != null && !operation.isEmpty()) {
                event.setOperation(operation);
            }
        }
        event.setLogger(logger);
        event.setMessageFormat(logEvent.getMessage());
        event.setMessageArgs(logEvent.getArgumentArray());
        try {
            etwProvider.writeEvent(event);
        } catch (ApplicationInsightsEtwException e) {
            addError("Exception from EtwProvider: " + e.getLocalizedMessage(), e);
        }
    }

}