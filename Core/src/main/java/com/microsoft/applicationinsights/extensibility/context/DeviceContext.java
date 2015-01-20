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

    String getId() {
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

    String getLanguage() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceLanguage());
    }

    public void setLanguage(String language) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceLanguage(), language);
    }

    String getRoleName()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceRoleName());
    }

    public void setRoleName(String roleName)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceRoleName(), roleName);
    }

    String getRoleInstance() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceRoleInstance());
    }

    public void setRoleInstance(String roleInstance) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceRoleInstance(), roleInstance);
    }

    public DeviceContext(ConcurrentMap<String, String> tags)
    {
        this.tags = tags;
    }
}