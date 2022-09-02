// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import java.util.concurrent.ConcurrentMap;

public final class CloudContext {

  private final ConcurrentMap<String, String> tags;

  public CloudContext(ConcurrentMap<String, String> tags) {
    this.tags = tags;
  }

  public void setRole(String role) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getCloudRole(), role);
  }

  public void setRoleInstance(String roleInstance) {
    MapUtil.setStringValueOrRemove(
        tags, ContextTagKeys.getKeys().getCloudRoleInstance(), roleInstance);
  }
}
