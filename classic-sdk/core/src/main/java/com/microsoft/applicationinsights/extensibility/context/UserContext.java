// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;

public final class UserContext {

  private final ConcurrentMap<String, String> tags;

  public UserContext(ConcurrentMap<String, String> tags) {
    this.tags = tags;
  }

  public void setId(String version) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserId(), version);
  }

  public void setAccountId(String version) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserAccountId(), version);
  }

  public void setUserAgent(String version) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserAgent(), version);
  }

  public void setAcquisitionDate(Date version) {
    MapUtil.setDateValueOrRemove(
        tags, ContextTagKeys.getKeys().getUserAccountAcquisitionDate(), version);
  }
}
