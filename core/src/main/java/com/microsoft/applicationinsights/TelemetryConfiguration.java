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

package com.microsoft.applicationinsights;

import com.azure.core.util.serializer.JacksonAdapter;
import com.azure.monitor.opentelemetry.exporter.implementation.ApplicationInsightsClientImpl;
import com.azure.monitor.opentelemetry.exporter.implementation.ApplicationInsightsClientImplBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.NdJsonSerializer;
import com.azure.monitor.opentelemetry.exporter.implementation.models.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString;
import com.microsoft.applicationinsights.internal.config.connection.EndpointProvider;
import com.microsoft.applicationinsights.internal.config.connection.InvalidConnectionStringException;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.apache.commons.text.StringSubstitutor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TelemetryConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryConfiguration.class);

    // Synchronization for instance initialization
    private static final Object s_lock = new Object();
    private static volatile TelemetryConfiguration active;

    private volatile String instrumentationKey;
    private volatile String connectionString;
    private volatile String roleName;
    private volatile String roleInstance;

    // cached based on instrumentationKey
    private volatile String eventTelemetryName;
    private volatile String exceptionTelemetryName;
    private volatile String messageTelemetryName;
    private volatile String metricTelemetryName;
    private volatile String pageViewTelemetryName;
    private volatile String remoteDependencyTelemetryName;
    private volatile String requestTelemetryName;

    private final EndpointProvider endpointProvider = new EndpointProvider();

    // globalTags contain:
    // * cloud role name
    // * cloud role instance
    // * sdk version
    // * application version (if provided in customDimensions)
    private final Map<String, String> globalTags;
    // contains customDimensions from json configuration
    private final Map<String, String> globalProperties;

    private final List<TelemetryModule> telemetryModules = new CopyOnWriteArrayList<>();

    private @Nullable ApplicationInsightsClientImpl channel;

    // only used by tests
    public TelemetryConfiguration() {
        this(new HashMap<>());
    }

    public TelemetryConfiguration(Map<String, String> customDimensions) {
        StringSubstitutor substitutor = new StringSubstitutor(System.getenv());
        Map<String, String> globalProperties = new HashMap<>();
        Map<String, String> globalTags = new HashMap<>();
        for (Map.Entry<String, String> entry : customDimensions.entrySet()) {
            String key = entry.getKey();
            if (key.equals("service.version")) {
                globalTags.put(ContextTagKeys.AI_APPLICATION_VER.toString(), substitutor.replace(entry.getValue()));
            } else {
                globalProperties.put(key, substitutor.replace(entry.getValue()));
            }
        }

        globalTags.put(ContextTagKeys.AI_INTERNAL_SDK_VERSION.toString(), PropertyHelper.getQualifiedSdkVersionString());

        this.globalProperties = globalProperties;
        this.globalTags = globalTags;
    }

    /**
     * Gets the active {@link com.microsoft.applicationinsights.TelemetryConfiguration} instance loaded from the
     * ApplicationInsights.xml file. If the configuration file does not exist, the active configuration instance is
     * initialized with minimum defaults needed to send telemetry to Application Insights.
     * @return The 'Active' instance
     */
    public static TelemetryConfiguration getActive() {
        if (active == null) {
            throw new IllegalStateException("agent was not initialized");
        }

        return active;
    }

    /**
     * This method provides the new instance of TelmetryConfiguration without loading the configuration
     * from configuration file. This will just give a plain bare bone instance. Typically used when
     * performing configuration programatically by creating beans, using @Beans tags. This is a common
     * scenario in SpringBoot.
     * @return {@link com.microsoft.applicationinsights.TelemetryConfiguration}
     */
    public static TelemetryConfiguration initActive(Map<String, String> customDimensions, ApplicationInsightsXmlConfiguration applicationInsightsConfig) {
        if (active != null) {
            throw new IllegalStateException("Already initialized");
        }
        if (active == null) {
            synchronized (s_lock) {
                if (active == null) {
                    active = new TelemetryConfiguration(customDimensions);
                    TelemetryConfigurationFactory.INSTANCE.initialize(active, applicationInsightsConfig);
                }
            }
        }
        return active;
    }

    /**
     * Gets the telemetry channel.
     */
    public synchronized ApplicationInsightsClientImpl getChannel() {
        if (channel == null) {
            channel = lazy();
        }
        return channel;
    }

    private ApplicationInsightsClientImpl lazy() {
        ApplicationInsightsClientImplBuilder restServiceClientBuilder = new ApplicationInsightsClientImplBuilder();

        // below copied from AzureMonitorExporterBuilder.java

        // FIXME (trask) NDJSON isn't working
        // Customize serializer to use NDJSON
        final SimpleModule ndjsonModule = new SimpleModule("Ndjson List Serializer");
        JacksonAdapter jacksonAdapter = new JacksonAdapter();
        jacksonAdapter.serializer().registerModule(ndjsonModule);
        ndjsonModule.addSerializer(new NdJsonSerializer());
        restServiceClientBuilder.serializerAdapter(jacksonAdapter);

        URI endpoint = endpointProvider.getIngestionEndpoint();
        try {
            URI hostOnly = new URI(endpoint.getScheme(), endpoint.getUserInfo(), endpoint.getHost(), endpoint.getPort(), null, null, null);
            restServiceClientBuilder.host(hostOnly.toString());
        } catch (URISyntaxException e) {
            // TODO (trask) revisit what's an appropriate action here?
            logger.error(e.getMessage(), e);
        }

        return restServiceClientBuilder.buildClient();
    }

    // this method only exists for generating bytecode via ASMifier in TelemetryClientClassFileTransformer
    @Deprecated
    public boolean isTrackingDisabled() {
        return true;
    }

    public List<TelemetryModule> getTelemetryModules() {
        return telemetryModules;
    }

    /**
     * Gets or sets the default instrumentation key for the application.
     */
    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    /**
     * Gets or sets the default instrumentation key for the application.
     */
    public void setInstrumentationKey(String key) {

        // A non null, non empty instrumentation key is a must
        if (Strings.isNullOrEmpty(key)) {
            throw new IllegalArgumentException("key");
        }

        instrumentationKey = key;

        String formattedInstrumentationKey = instrumentationKey.replaceAll("-", "");
        eventTelemetryName = "Microsoft.ApplicationInsights." + formattedInstrumentationKey + ".Event";
        exceptionTelemetryName = "Microsoft.ApplicationInsights." + formattedInstrumentationKey + ".Exception";
        messageTelemetryName = "Microsoft.ApplicationInsights." + formattedInstrumentationKey + ".Message";
        metricTelemetryName = "Microsoft.ApplicationInsights." + formattedInstrumentationKey + ".Metric";
        pageViewTelemetryName = "Microsoft.ApplicationInsights." + formattedInstrumentationKey + ".PageView";
        remoteDependencyTelemetryName = "Microsoft.ApplicationInsights." + formattedInstrumentationKey + ".RemoteDependency";
        requestTelemetryName = "Microsoft.ApplicationInsights." + formattedInstrumentationKey + ".Request";
    }

    public @Nullable String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
        globalTags.put(ContextTagKeys.AI_CLOUD_ROLE.toString(), roleName);
    }

    public String getRoleInstance() {
        return roleInstance;
    }

    public void setRoleInstance(String roleInstance) {
        this.roleInstance = roleInstance;
        globalTags.put(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString(), roleInstance);
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        try {
            ConnectionString.parseInto(connectionString, this);
        } catch (InvalidConnectionStringException e) {
            throw new IllegalArgumentException("Invalid connection string", e);
        }
        this.connectionString = connectionString;
    }

    public EndpointProvider getEndpointProvider() {
        return endpointProvider;
    }

    public void initEventTelemetry(TelemetryItem telemetry, TelemetryEventData data) {
        initTelemetry(telemetry, data, eventTelemetryName, "EventData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    public void initExceptionTelemetry(TelemetryItem telemetry, TelemetryExceptionData data) {
        initTelemetry(telemetry, data, exceptionTelemetryName, "ExceptionData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    public void initMessageTelemetry(TelemetryItem telemetry, MessageData data) {
        initTelemetry(telemetry, data, messageTelemetryName, "MessageData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    // FIXME (trask) rename MetricsData to MetricData to match the telemetryName and baseType?
    public void initMetricTelemetry(TelemetryItem telemetry, MetricsData data) {
        initTelemetry(telemetry, data, metricTelemetryName, "MetricData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    public void initPageViewTelemetry(TelemetryItem telemetry, PageViewData data) {
        initTelemetry(telemetry, data, pageViewTelemetryName, "PageViewData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    public void initRemoteDependencyTelemetry(TelemetryItem telemetry, RemoteDependencyData data) {
        initTelemetry(telemetry, data, remoteDependencyTelemetryName, "RemoteDependencyData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    public void initRequestTelemetry(TelemetryItem telemetry, RequestData data) {
        initTelemetry(telemetry, data, requestTelemetryName, "RequestData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    private void initTelemetry(TelemetryItem telemetry, MonitorDomain data, String telemetryName, String baseType) {
        telemetry.setVersion(1);
        telemetry.setName(telemetryName);
        telemetry.setInstrumentationKey(instrumentationKey);
        telemetry.setTags(new HashMap<>(globalTags));

        data.setVersion(2);

        MonitorBase monitorBase = new MonitorBase();
        telemetry.setData(monitorBase);
        monitorBase.setBaseType(baseType);
        monitorBase.setBaseData(data);
    }
}
