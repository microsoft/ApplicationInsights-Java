// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import java.util.concurrent.ConcurrentMap;

public final class DeviceContext {

  private final ConcurrentMap<String, String> tags;

  public DeviceContext(ConcurrentMap<String, String> tags) {
    this.tags = tags;
  }

  public void setType(String type) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceType(), type);
  }

  public void setId(String id) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceId(), id);
  }

  public void setOperatingSystem(String operatingSystem) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceOS(), operatingSystem);
  }

  public void setOperatingSystemVersion(String operatingSystemVersion) {
    MapUtil.setStringValueOrRemove(
        tags, ContextTagKeys.getKeys().getDeviceOSVersion(), operatingSystemVersion);
  }

  public void setOemName(String oemName) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceOEMName(), oemName);
  }

  public void setModel(String model) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceModel(), model);
  }

  public void setNetworkType(String networkType) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceNetwork(), networkType);
  }

  public void setScreenResolution(String screenResolution) {
    MapUtil.setStringValueOrRemove(
        tags, ContextTagKeys.getKeys().getDeviceScreenResolution(), screenResolution);
  }

  public void setLocale(String locale) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceLocale(), locale);
  }

  public void setLanguage(String language) {
    MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceLanguage(), language);
  }
}
