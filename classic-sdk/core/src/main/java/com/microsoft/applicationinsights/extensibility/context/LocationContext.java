// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public final class LocationContext {

  private static final Pattern PATTERN =
      Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

  private final ConcurrentMap<String, String> tags;

  public LocationContext(ConcurrentMap<String, String> tags) {
    this.tags = tags;
  }

  public void setIp(String value) {
    if (!LocalStringsUtils.isNullOrEmpty(value) && isIpV4(value)) {
      MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getLocationIP(), value);
    }
  }

  private static boolean isIpV4(String ip) {
    return PATTERN.matcher(ip).matches();
  }
}
