package com.microsoft.applicationinsights.implementation;

import java.io.IOException;
import java.util.Map;

import com.microsoft.applicationinsights.datacontracts.*;
import com.microsoft.applicationinsights.extensibility.model.ContextTagKeys;
import com.microsoft.applicationinsights.util.LocalStringsUtils;
import com.microsoft.applicationinsights.util.MapUtil;

public class DeviceContext implements JsonSerializable
{
    private final Map<String, String> tags;

    String getType()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceType());
    }

    public void setType(String type)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceType(), type);
    }

    String getId()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceId());
    }

    public void setId(String id)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceId(), id);
    }

    String getOperatingSystem()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceOS());
    }

    public void setOperatingSystem(String operatingSystem)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceOS(), operatingSystem);
    }

    String getOemName()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceOEMName());
    }

    public void setOemName(String oemName)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceOEMName(), oemName);
    }

    String getModel()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceModel());
    }

    public void setModel(String model)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceModel(), model);
    }

    String getNetworkType()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceNetwork());
    }

    public void setNetworkType(String networkType)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceNetwork(), networkType);
    }

    String getScreenResolution()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceScreenResolution());
    }

    public void setScreenResolution(String screenResolution)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceScreenResolution(), screenResolution);
    }

    String getLanguage()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceLanguage());
    }

    public void setLanguage(String language)
    {
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

    String getRoleInstance()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getDeviceRoleInstance());
    }

    public void setRoleInstance(String roleInstance)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getDeviceRoleInstance(), roleInstance);
    }

    public DeviceContext(Map<String, String> tags)
    {
        this.tags = tags;
    }

    @Override
    public void serialize(com.microsoft.applicationinsights.datacontracts.JsonWriter writer) throws IOException
    {
        writer.writeStartObject();
        writer.writeProperty("type", this.getType());
        writer.writeProperty("id", LocalStringsUtils.populateRequiredStringWithNullValue(this.getId(), "id", DeviceContext.class.getName()));
        writer.writeProperty("osVersion", this.getOperatingSystem());
        writer.writeProperty("oemName", this.getOemName());
        writer.writeProperty("model", this.getModel());
        writer.writeProperty("network", this.getNetworkType());
        writer.writeProperty("resolution", this.getScreenResolution());
        writer.writeProperty("locale", this.getLanguage());
        writer.writeProperty("roleName", this.getRoleName());
        writer.writeProperty("roleInstance", this.getRoleInstance());
        writer.writeEndObject();
    }
}