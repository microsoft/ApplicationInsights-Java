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

package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class Device.
 */
public class Device implements JsonSerializable {
    /**
     * Backing field for property Id.
     */
    private String id;

    /**
     * Backing field for property Ip.
     */
    private String ip;

    /**
     * Backing field for property Language.
     */
    private String language;

    /**
     * Backing field for property Locale.
     */
    private String locale;

    /**
     * Backing field for property Model.
     */
    private String model;

    /**
     * Backing field for property Network.
     */
    private String network;

    /**
     * Backing field for property OemName.
     */
    private String oemName;

    /**
     * Backing field for property Os.
     */
    private String os;

    /**
     * Backing field for property OsVersion.
     */
    private String osVersion;

    /**
     * Backing field for property RoleInstance.
     */
    private String roleInstance;

    /**
     * Backing field for property RoleName.
     */
    private String roleName;

    /**
     * Backing field for property ScreenResolution.
     */
    private String screenResolution;

    /**
     * Backing field for property Type.
     */
    private String type;

    /**
     * Backing field for property VmName.
     */
    private String vmName;

    /**
     * Initializes a new instance of the class.
     */
    public Device() {
        this.InitializeFields();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String value) {
        this.ip = value;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String value) {
        this.language = value;
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLocale(String value) {
        this.locale = value;
    }

    public String getModel() {
        return this.model;
    }

    public void setModel(String value) {
        this.model = value;
    }

    public String getNetwork() {
        return this.network;
    }

    public void setNetwork(String value) {
        this.network = value;
    }

    public String getOemName() {
        return this.oemName;
    }

    public void setOemName(String value) {
        this.oemName = value;
    }

    public String getOs() {
        return this.os;
    }

    public void setOs(String value) {
        this.os = value;
    }

    public String getOsVersion() {
        return this.osVersion;
    }

    public void setOsVersion(String value) {
        this.osVersion = value;
    }

    public String getRoleInstance() {
        return this.roleInstance;
    }

    public void setRoleInstance(String value) {
        this.roleInstance = value;
    }

    public String getRoleName() {
        return this.roleName;
    }

    public void setRoleName(String value) {
        this.roleName = value;
    }

    public String getScreenResolution() {
        return this.screenResolution;
    }

    public void setScreenResolution(String value) {
        this.screenResolution = value;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public String getVmName() {
        return this.vmName;
    }

    public void setVmName(String value) {
        this.vmName = value;
    }

    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (this.id != null) {
            map.put("id", this.id);
        }
        if (this.ip != null) {
            map.put("ip", this.ip);
        }
        if (this.language != null) {
            map.put("language", this.language);
        }
        if (this.locale != null) {
            map.put("locale", this.locale);
        }
        if (this.model != null) {
            map.put("model", this.model);
        }
        if (this.network != null) {
            map.put("network", this.network);
        }
        if (this.oemName != null) {
            map.put("oemName", this.oemName);
        }
        if (this.os != null) {
            map.put("os", this.os);
        }
        if (this.osVersion != null) {
            map.put("osVersion", this.osVersion);
        }
        if (this.roleInstance != null) {
            map.put("roleInstance", this.roleInstance);
        }
        if (this.roleName != null) {
            map.put("roleName", this.roleName);
        }
        if (this.screenResolution != null) {
            map.put("screenResolution", this.screenResolution);
        }
        if (this.type != null) {
            map.put("type", this.type);
        }
        if (this.vmName != null) {
            map.put("vmName", this.vmName);
        }
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
     }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be throw during serialization.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("id", id);
        writer.write("ip", ip);
        writer.write("language", language);
        writer.write("locale", locale);
        writer.write("model", model);
        writer.write("locale", locale);
        writer.write("network", network);
        writer.write("oemName", oemName);
        writer.write("os", os);
        writer.write("osVersion", osVersion);
        writer.write("roleInstance", roleInstance);
        writer.write("roleName", roleName);
        writer.write("screenResolution", screenResolution);
        writer.write("type", type);
        writer.write("vmName", vmName);
    }

    protected void InitializeFields() {
    }
}
