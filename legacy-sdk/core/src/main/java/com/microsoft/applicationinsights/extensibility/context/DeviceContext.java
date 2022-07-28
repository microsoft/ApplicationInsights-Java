/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.MapUtil;
import java.util.concurrent.ConcurrentMap;

public final class DeviceContext {

  private final ConcurrentMap<String, String> tags;

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

  public DeviceContext(ConcurrentMap<String, String> tags) {
    this.tags = tags;
  }
}
