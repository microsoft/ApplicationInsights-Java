// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model;

public abstract class IpaEtwEventErrorBase extends IpaEtwEventBase {
  private String stacktrace;

  protected IpaEtwEventErrorBase() {
    super();
  }

  protected IpaEtwEventErrorBase(IpaEtwEventBase evt) {
    super(evt);
    if (evt instanceof IpaEtwEventErrorBase) {
      setStacktrace(((IpaEtwEventErrorBase) evt).stacktrace);
    }
  }

  @Override
  protected String processMessageFormat() {
    if (stacktrace == null || stacktrace.isEmpty()) {
      return super.processMessageFormat();
    } else {
      return super.processMessageFormat() + "\n" + this.stacktrace;
    }
  }

  public void setStacktrace(String stacktrace) {
    this.stacktrace = stacktrace;
  }
}
