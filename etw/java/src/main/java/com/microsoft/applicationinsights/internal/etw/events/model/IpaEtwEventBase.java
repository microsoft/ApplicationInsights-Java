package com.microsoft.applicationinsights.internal.etw.events.model;

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
    }

    public String getLogger() {
        return StringUtils.defaultString(logger);
    }

    /**
     * @param logger the logger to set
     */
    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getExtensionVersion() {
        return StringUtils.defaultString(extensionVersion);
    }

    /**
     * @param extensionVersion the extensionVersion to set
     */
    public void setExtensionVersion(String extensionVersion) {
        this.extensionVersion = extensionVersion;
    }

    public String getAppName() {
        return StringUtils.defaultString(appName);
    }

    /**
     * @param appName the appName to set
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getResourceType() {
        return StringUtils.defaultString(resourceType);
    }

    /**
     * @param resourceType the resourceType to set
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getInstrumentationKey() {
        return StringUtils.defaultString(instrumentationKey);
    }

    /**
     * @param instrumentationKey the instrumentationKey to set
     */
    public void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }

    public String getSubscriptionId() {
        return StringUtils.defaultString(subscriptionId);
    }

    /**
     * @param subscriptionId the subscriptionId to set
     */
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    /**
     * @param messageFormat the messageFormat to set
     */
    public void setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    /**
     * @param messageArgs the messageArgs to set
     */
    public void setMessageArgs(Object... messageArgs) {
        this.messageArgs = messageArgs;
    }

    public String getFormattedMessage() {
        if (messageArgs.length == 0) {
            return StringUtils.defaultString(messageFormat);
        } else {
            return String.format(messageFormat, messageArgs);
        }
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return StringUtils.defaultString(operation);
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }
}