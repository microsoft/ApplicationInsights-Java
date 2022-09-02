// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventId;

public class IpaInfo extends IpaEtwEventBase {

  public IpaInfo() {
    super();
  }

  public IpaInfo(IpaEtwEventBase event) {
    super(event);
  }

  @Override
  public IpaEtwEventId id() {
    return IpaEtwEventId.INFO;
  }
}
