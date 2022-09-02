// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventBase;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventErrorBase;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.events.model.IpaEtwEventId;

/** JavaIpaCritical. */
public class IpaCritical extends IpaEtwEventErrorBase {

  public IpaCritical() {
    super();
  }

  public IpaCritical(IpaEtwEventBase event) {
    super(event);
  }

  @Override
  public IpaEtwEventId id() {
    return IpaEtwEventId.CRITICAL;
  }
}
