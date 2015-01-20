package com.microsoft.applicationinsights.telemetry;

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
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Represents a context for sending telemetry to the Application Insights service.
 */
public final class TelemetryContext {
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

    /**
     * Default Ctor
     */
    public TelemetryContext() {
        this(new ConcurrentHashMap<String, String>(), new ConcurrentHashMap<String, String>());
    }

    /**
     * Gets the object describing the component tracked by this instance.
     * @return The component.
     */
    public ComponentContext getComponent() {
        if (component == null) {
            component = new ComponentContext(tags);
        }

        return component;
    }

    /**
     * Gets the object describing the device tracked by this instance.
     * @return The device.
     */
    public DeviceContext getDevice() {
        if (device == null) {
            device = new DeviceContext(tags);
        }

        return device;
    }

    /**
     * Gets the object describing a user session tracked by this instance.
     * @return The user's session.
     */
    public SessionContext getSession() {
        if (session == null) {
            session = new SessionContext(tags);
        }

        return session;
    }

    /**
     * Gets the object describing a user tracked by this instance.
     * @return The user.
     */
    public UserContext getUser() {
        if (user == null) {
            user = new UserContext(tags);
        }

        return user;
    }

    /**
     * Gets the object describing a operation tracked by this instance.
     * @return The operation.
     */
    public OperationContext getOperation() {
        if (operation == null) {
            operation = new OperationContext(tags);
        }

        return operation;
    }

    /**
     *Gets the object describing a location tracked by this instance.
     * @return The location.
     */
    public LocationContext getLocation() {
        if (location == null) {
            location = new LocationContext(tags);
        }

        return location;
    }

    /**
     * Gets the default instrumentation key for all {@link com.microsoft.applicationinsights.telemetry.Telemetry}
     * objects logged in this {@link com.microsoft.applicationinsights.telemetry.TelemetryContext}.
     *
     * By default, this property is initialized with the InstrumentationKey value which is in
     * {@link com.microsoft.applicationinsights.TelemetryConfiguration} of the 'Active' instance.
     *
     * You can specify it for all telemetry tracked via a particular {@link com.microsoft.applicationinsights.TelemetryClient}
     * or for a specific {@link com.microsoft.applicationinsights.telemetry.Telemetry}
     *
     * @return The instrumentation key
     */
    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    /**
     * Sets the default instrumentation key for all {@link com.microsoft.applicationinsights.telemetry.Telemetry}
     * objects logged in this {@link com.microsoft.applicationinsights.telemetry.TelemetryContext}.
     *
     * By default, this property is initialized with the InstrumentationKey value which is in
     * {@link com.microsoft.applicationinsights.TelemetryConfiguration} of the 'Active' instance.
     *
     * You can specify it for all telemetry tracked via a particular {@link com.microsoft.applicationinsights.TelemetryClient}
     * or for a specific {@link com.microsoft.applicationinsights.telemetry.Telemetry}
     *
     * @param instrumentationKey The instrumentation key
     */
    public void setInstrumentationKey(String instrumentationKey) {
        if (!Sanitizer.isUUID(instrumentationKey)) {
            InternalLogger.INSTANCE.log("Telemetry Configuration: illegal instrumentation key: %s", instrumentationKey);
        }

        this.instrumentationKey = instrumentationKey;
    }

    /**
     * Gets a dictionary of application-defined property values.
     * @return The application-defined property values.
     */
    public ConcurrentMap<String, String> getProperties() {
        return properties;
    }

    /**
     * Gets a dictionary of context tags.
     * @return The tags.
     */
    public ConcurrentMap<String, String> getTags() {
        return tags;
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

    public InternalContext getInternal() {
        if (internal == null) {
            internal = new InternalContext(tags);
        }

        return internal;
    }

    TelemetryContext(ConcurrentMap<String, String> properties, ConcurrentMap<String, String> tags) {
        if (properties == null) {
            throw new IllegalArgumentException("properties cannot be null");
        }

        if (tags == null) {
            throw new IllegalArgumentException("tags cannot be null");
        }

        this.properties = properties;
        this.tags = tags;
    }
}
