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
package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model;

import org.apache.commons.lang3.StringUtils;

public abstract class IpaEtwEventBase implements IpaEtwEvent {
    private String extensionVersion;
    private String appName;
    private String resourceType;
    private String instrumentationKey;
    private String subscriptionId;

    // not copied from prototype
    private String logger;
    private String messageFormat;
    private Object[] messageArgs = new Object[0];
    private String operation;

    public IpaEtwEventBase() {
    }

    public IpaEtwEventBase(IpaEtwEventBase event) {
        extensionVersion = event.extensionVersion;
        appName = event.appName;
        resourceType = event.resourceType;
        instrumentationKey = event.instrumentationKey;
        subscriptionId = event.subscriptionId;

        logger = event.logger;
        messageFormat = event.messageFormat;
        messageArgs = event.messageArgs;
        operation = event.operation;
    }

    public String getLogger() {
        return StringUtils.defaultString(logger);
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getExtensionVersion() {
        return StringUtils.defaultString(extensionVersion);
    }

    public void setExtensionVersion(String extensionVersion) {
        this.extensionVersion = extensionVersion;
    }

    public String getAppName() {
        return StringUtils.defaultString(appName);
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getResourceType() {
        return StringUtils.defaultString(resourceType);
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getInstrumentationKey() {
        return StringUtils.defaultString(instrumentationKey);
    }

    public void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }

    public String getSubscriptionId() {
        return StringUtils.defaultString(subscriptionId);
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    public void setMessageArgs(Object... messageArgs) {
        this.messageArgs = messageArgs;
    }

    public String getFormattedMessage() {
        if (messageArgs == null || messageArgs.length == 0) {
            return StringUtils.defaultString(messageFormat);
        } else {
            return String.format(messageFormat, messageArgs);
        }
    }

    public String getOperation() {
        return StringUtils.defaultString(operation);
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}