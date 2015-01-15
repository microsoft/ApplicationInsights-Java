package com.microsoft.applicationinsights.telemetry;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.extensibility.context.ComponentContext;
import com.microsoft.applicationinsights.extensibility.context.DeviceContext;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.extensibility.context.LocationContext;
import com.microsoft.applicationinsights.extensibility.context.InternalContext;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.MapUtil;

import com.google.common.base.Strings;

/**
 * Represents a context for sending telemetry to the Application Insights service.
 */
public final class TelemetryContext implements JsonSerializable {
    private ConcurrentMap<String,String> properties;
    private ConcurrentMap<String,String> tags;

    private String instrumentationKey;
    private ComponentContext component;
    private DeviceContext device;
    private SessionContext session;
    private UserContext user;
    private OperationContext operation;
    private LocationContext location;
    private InternalContext internal;

    public TelemetryContext() {
        this(new ConcurrentHashMap<String, String>(), new ConcurrentHashMap<String, String>());
    }

    public TelemetryContext(ConcurrentMap<String, String> properties, ConcurrentMap<String, String> tags) {
        if (properties == null) {
            throw new IllegalArgumentException("properties cannot be null");
        }

        if (tags == null) {
            throw new IllegalArgumentException("tags cannot be null");
        }

        this.properties = properties;
        this.tags = tags;
    }

    public ComponentContext getComponent() {
        if (component == null) {
            component = new ComponentContext(tags);
        }

        return component;
    }

    public DeviceContext getDevice() {
        if (device == null) {
            device = new DeviceContext(tags);
        }

        return device;
    }

    public SessionContext getSession() {
        if (session == null) {
            session = new SessionContext(tags);
        }

        return session;
    }

    public UserContext getUser() {
        if (user == null) {
            user = new UserContext(tags);
        }

        return user;
    }

    public OperationContext getOperation() {
        if (operation == null) {
            operation = new OperationContext(tags);
        }

        return operation;
    }

    public LocationContext getLocation() {
        if (location == null) {
            location = new LocationContext(tags);
        }

        return location;
    }

    public InternalContext getInternal() {
        if (internal == null) {
            internal = new InternalContext(tags);
        }

        return internal;
    }

    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    public boolean setInstrumentationKey(String instrumentationKey) {
        if (!Sanitizer.isUUID(instrumentationKey)) {
            InternalLogger.INSTANCE.log("Telemetry Configuration: illegal instrumentation key: %s ignored", instrumentationKey);
            return false;
        }

        this.instrumentationKey = instrumentationKey;
        return true;
    }

    public ConcurrentMap<String, String> getProperties() {
        return properties;
    }

    public ConcurrentMap<String, String> getTags() {
        return tags;
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("iKey", this.getInstrumentationKey());
        writer.write("device", this.getDevice());
        writer.write("application", this.getComponent());
        writer.write("user", this.getUser());
        writer.write("operation", this.getOperation());
        writer.write("session", this.getSession());
        writer.write("location", this.getLocation());
        writer.write("internal", this.getInternal());
    }

    public void initialize(TelemetryContext source) {
        if (!Strings.isNullOrEmpty(source.getInstrumentationKey()))
            setInstrumentationKey(source.getInstrumentationKey());

        if (source.tags != null && source.tags.size() > 0) {
            MapUtil.copy(source.tags, this.tags);
        }
        if (source.properties != null && source.properties.size() > 0) {
            MapUtil.copy(source.properties, this.properties);
        }
    }
}
