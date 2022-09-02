// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model;

public abstract class IpaEtwEventBase implements IpaEtwEvent {
  private String extensionVersion;
  private String appName;
  private String instrumentationKey;
  private String subscriptionId;
  private String msgId;

  // not copied from prototype
  private String logger;
  private String messageFormat;
  private Object[] messageArgs = new Object[0];
  private String operation;

  protected IpaEtwEventBase() {}

  protected IpaEtwEventBase(IpaEtwEventBase event) {
    extensionVersion = event.extensionVersion;
    appName = event.appName;
    instrumentationKey = event.instrumentationKey;
    subscriptionId = event.subscriptionId;
    msgId = event.msgId;
  }

  public void setLogger(String logger) {
    this.logger = logger;
  }

  // used by native method
  public String getExtensionVersion() {
    return extensionVersion == null ? "" : extensionVersion;
  }

  public void setExtensionVersion(String extensionVersion) {
    this.extensionVersion = extensionVersion;
  }

  // used by native method
  public String getAppName() {
    return appName == null ? "" : appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  // used by native method
  public String getSubscriptionId() {
    return subscriptionId == null ? "" : subscriptionId;
  }

  public void setSubscriptionId(String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  // used by native method
  public String getInstrumentationKey() {
    return instrumentationKey == null ? "" : instrumentationKey;
  }

  public void setInstrumentationKey(String instrumentationKey) {
    this.instrumentationKey = instrumentationKey;
  }

  // used by native method
  public String getMsgId() {
    return msgId == null ? "" : msgId;
  }

  public void setMsgId(String msgId) {
    this.msgId = msgId;
  }

  public void setMessageFormat(String messageFormat) {
    this.messageFormat = messageFormat;
  }

  public void setMessageArgs(Object... messageArgs) {
    this.messageArgs = messageArgs;
  }

  // used by native method
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
      String prefix = "[" + this.logger;
      if (this.operation != null && !this.operation.isEmpty()) {
        prefix += "/" + this.operation;
      }
      prefix += "] ";
      return prefix + messageFormat;
    } else if (this.operation != null && !this.operation.isEmpty()) {
      return "[-/" + this.operation + "] " + messageFormat;
    } else {
      return messageFormat;
    }
  }

  // used by native method
  public String getOperation() {
    return operation == null ? "" : operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }
}
