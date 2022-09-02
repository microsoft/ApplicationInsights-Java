// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import java.util.concurrent.ConcurrentMap;

public final class SessionContext {

  private final ConcurrentMap<String, String> tags;

  public SessionContext(ConcurrentMap<String, String> tags) {
    this.tags = tags;
  }

  public void setId(String id) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getSessionId(), id);
  }

  public void setIsFirst(Boolean isFirst) {
    MapUtil.setBoolValueOrRemove(tags, ContextTagKeys.getKeys().getSessionIsFirst(), isFirst);
  }

  public void setIsNewSession(Boolean isNewSession) {
    MapUtil.setBoolValueOrRemove(tags, ContextTagKeys.getKeys().getSessionIsNew(), isNewSession);
  }
}
