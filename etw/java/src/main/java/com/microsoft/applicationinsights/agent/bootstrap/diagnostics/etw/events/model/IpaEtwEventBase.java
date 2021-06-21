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

public abstract class IpaEtwEventBase implements IpaEtwEvent {
    private String extensionVersion;
    private String appName;
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
        instrumentationKey = event.instrumentationKey;
        subscriptionId = event.subscriptionId;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getExtensionVersion() {
        return extensionVersion == null ? "" : extensionVersion;
    }

    public void setExtensionVersion(String extensionVersion) {
        this.extensionVersion = extensionVersion;
    }

    public String getAppName() {
        return appName == null ? "" : appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getSubscriptionId() {
        return subscriptionId == null ? "" : subscriptionId;
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
        // operation
        // logger
        // exception (in error class)
        String fmt = processMessageFormat();
        if (messageArgs == null || messageArgs.length == 0) {
            return fmt == null ? "" : fmt;
        } else {
            return String.format(fmt, messageArgs);
        }
    }

    protected String processMessageFormat() {
        if (this.logger != null && !this.logger.isEmpty()) {
            String prefix = "["+this.logger;
            if (this.operation != null && !this.operation.isEmpty()) {
                prefix += "/"+this.operation;
            }
            prefix += "] ";
            return prefix + messageFormat;
        } else if(this.operation != null && !this.operation.isEmpty()) {
            return "[-/"+this.operation+"] "+messageFormat;
        } else {
            return messageFormat;
        }
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}