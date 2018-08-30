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

import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class DeviceContext {
    private final ConcurrentMap<String, String> tags;

    String getType()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceType());
    }

    public void setType(String type) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceType(), type);
    }

    public String getId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceId());
    }

    public void setId(String id) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceId(), id);
    }

    String getOperatingSystem() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceOS());
    }

    public void setOperatingSystem(String operatingSystem) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceOS(), operatingSystem);
    }

    String getOperatingSystemVersion() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceOSVersion());
    }

    public void setOperatingSystemVersion(String operatingSystemVersion) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceOSVersion(), operatingSystemVersion);
    }

    String getOemName() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceOEMName());
    }

    public void setOemName(String oemName) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceOEMName(), oemName);
    }

    String getModel() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceModel());
    }

    public void setModel(String model) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceModel(), model);
    }

    String getNetworkType() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceNetwork());
    }

    public void setNetworkType(String networkType) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceNetwork(), networkType);
    }

    String getScreenResolution() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceScreenResolution());
    }

    public void setScreenResolution(String screenResolution) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceScreenResolution(), screenResolution);
    }

    String getLocale() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceLocale());
    }

    public void setLocale(String locale) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceLocale(), locale);
    }

    String getLanguage() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceLanguage());
    }

    public void setLanguage(String language) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceLanguage(), language);
    }

    /**
     * @deprecated use {@link CloudContext#getRole()}
     */
    @Deprecated
    String getRoleName()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceRoleName());
    }

    /**
     * @deprecated use {@link CloudContext#setRole(String)}
     */
    @Deprecated
    public void setRoleName(String roleName)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceRoleName(), roleName);
    }

    /**
     * @deprecated use {@link CloudContext#getRoleInstance()}
     */
    @Deprecated
    String getRoleInstance() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceRoleInstance());
    }

    /**
     * @deprecated use {@link CloudContext#setRoleInstance(String)}
     */
    @Deprecated
    public void setRoleInstance(String roleInstance) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceRoleInstance(), roleInstance);
    }

    public DeviceContext(ConcurrentMap<String, String> tags)
    {
        this.tags = tags;
    }
}