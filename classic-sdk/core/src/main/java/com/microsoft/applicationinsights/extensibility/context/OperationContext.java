// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import java.util.Map;

public final class OperationContext {

  private final Map<String, String> tags;

  public OperationContext(Map<String, String> tags) {
    this.tags = tags;
  }

  public String getId() {
    // note: this method is instrumented by the Javaagent
    return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getOperationId());
  }

  public void setId(String id) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getOperationId(), id);
  }

  public void setParentId(String parentId) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getOperationParentId(), parentId);
  }

  public void setName(String name) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getOperationName(), name);
  }

  public void setSyntheticSource(String syntheticSource) {
    MapUtil.setStringValueOrRemove(
        tags, ContextTagKeys.getKeys().getSyntheticSource(), syntheticSource);
  }
}
