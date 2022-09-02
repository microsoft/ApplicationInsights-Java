// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import java.util.concurrent.ConcurrentMap;

public final class ComponentContext {

  private final ConcurrentMap<String, String> tags;

  public ComponentContext(ConcurrentMap<String, String> tags) {
    this.tags = tags;
  }

  public void setVersion(String version) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getApplicationVersion(), version);
  }
}
