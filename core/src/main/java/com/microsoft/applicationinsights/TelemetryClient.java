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

import com.azure.monitor.opentelemetry.exporter.implementation.models.*;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.TelemetryClientInitializer;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString;
import com.microsoft.applicationinsights.internal.config.connection.EndpointProvider;
import com.microsoft.applicationinsights.internal.config.connection.InvalidConnectionStringException;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import io.opentelemetry.sdk.common.CompletableResultCode;
import org.apache.commons.text.StringSubstitutor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.singletonList;

import java.util.Map;
import java.util.stream.Collectors;

import com.microsoft.applicationinsights.internal.perfcounter.Constants;

import static java.util.Arrays.asList;

public class TelemetryClient {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);

    // Synchronization for instance initialization
    private static final Object s_lock = new Object();
    private static volatile TelemetryClient active;

    private static final Set<String> BUILT_IN_METRIC_NAMES =
            new HashSet<>(asList(
                    Constants.TOTAL_CPU_PC_METRIC_NAME,
                    Constants.PROCESS_CPU_PC_METRIC_NAME,
                    Constants.PROCESS_MEM_PC_METRICS_NAME,
                    Constants.TOTAL_MEMORY_PC_METRIC_NAME,
                    Constants.PROCESS_IO_PC_METRIC_NAME));

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

    private final List<MetricFilter> metricFilters;

    private final List<TelemetryModule> telemetryModules = new CopyOnWriteArrayList<>();

    private final Object channelInitLock = new Object();
    private volatile @Nullable BatchSpanProcessor channelBatcher;

    // only used by tests
    public TelemetryClient() {
        this(new HashMap<>(), new ArrayList<>());
    }

    public TelemetryClient(Map<String, String> customDimensions, List<MetricFilter> metricFilters) {
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
        this.metricFilters = metricFilters;
    }

    /**
     * Gets the active {@link TelemetryClient} instance loaded from the
     * ApplicationInsights.xml file. If the configuration file does not exist, the active configuration instance is
     * initialized with minimum defaults needed to send telemetry to Application Insights.
     * @return The 'Active' instance
     */
    public static TelemetryClient getActive() {
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
     * @return {@link TelemetryClient}
     */
    public static TelemetryClient initActive(Map<String, String> customDimensions, List<MetricFilter> metricFilters, ApplicationInsightsXmlConfiguration applicationInsightsConfig) {
        if (active != null) {
            throw new IllegalStateException("Already initialized");
        }
        if (active == null) {
            synchronized (s_lock) {
                if (active == null) {
                    TelemetryClient active = new TelemetryClient(customDimensions, metricFilters);
                    TelemetryClientInitializer.INSTANCE.initialize(active, applicationInsightsConfig);
                    TelemetryClient.active = active;
                }
            }
        }
        return active;
    }

    public void trackAsync(TelemetryItem telemetry) {

        MonitorDomain data = telemetry.getData().getBaseData();
        if (data instanceof MetricsData) {
            MetricsData metricsData = (MetricsData) data;
            List<MetricDataPoint> filteredPoints = metricsData.getMetrics().stream().filter(point -> {
                String metricName = point.getName();
                if (BUILT_IN_METRIC_NAMES.contains(metricName)) {
                    return true;
                }
                for (MetricFilter metricFilter : metricFilters) {
                    if (!metricFilter.matches(metricName)) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());

            if (filteredPoints.isEmpty()) {
                return;
            }
            metricsData.setMetrics(filteredPoints);
        }

        if (telemetry.getSampleRate() == null) {
            // FIXME (trask) is this required?
            telemetry.setSampleRate(100f);
        }

        if (telemetry.getTime() == null) {
            // TODO (trask) remove this after confident no code paths hit this
            throw new IllegalArgumentException("telemetry item is missing time");
        }

        QuickPulseDataCollector.INSTANCE.add(telemetry);

        TelemetryObservers.INSTANCE.getObservers().forEach(consumer -> consumer.accept(telemetry));

        // batching, retry, throttling, and writing to disk on failure occur downstream
        // for simplicity not reporting back success/failure from this layer
        // only that it was successfully delivered to the next layer
        getChannelBatcher().trackAsync(telemetry);
    }

    public CompletableResultCode flushChannelBatcher() {
        return channelBatcher.forceFlush();
    }

    public BatchSpanProcessor getChannelBatcher() {
        if (channelBatcher == null) {
            synchronized (channelInitLock) {
                if (channelBatcher == null) {
                    TelemetryChannel channel;
                    try {
                        channel = new TelemetryChannel(endpointProvider.getIngestionEndpoint().toURL());
                    } catch (MalformedURLException e) {
                        // this shouldn't happen
                        logger.error(e.getMessage(), e);
                        throw new IllegalStateException(e);
                    }
                    channelBatcher = BatchSpanProcessor.builder(channel).build();
                }
            }
        }
        return channelBatcher;
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

    public List<MetricFilter> getMetricFilters() {
        return metricFilters;
    }

    public void setMetricFilters(List<MetricFilter> metricFilters) {
        this.metricFilters.clear();
        this.metricFilters.addAll(metricFilters);
    }

    public EndpointProvider getEndpointProvider() {
        return endpointProvider;
    }

    // must be called before setting any telemetry tags or data properties
    //
    // telemetry tags will be non-null after this call
    // data properties may or may not be non-null after this call
    public void initEventTelemetry(TelemetryItem telemetry, TelemetryEventData data) {
        if (telemetry.getTags() != null) {
            throw new AssertionError("must not set telemetry tags before calling init");
        }
        if (data.getProperties() != null) {
            throw new AssertionError("must not set data properties before calling init");
        }
        initTelemetry(telemetry, data, eventTelemetryName, "EventData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    // must be called before setting any telemetry tags or data properties
    //
    // telemetry tags will be non-null after this call
    // data properties may or may not be non-null after this call
    public void initExceptionTelemetry(TelemetryItem telemetry, TelemetryExceptionData data) {
        if (telemetry.getTags() != null) {
            throw new AssertionError("must not set telemetry tags before calling init");
        }
        if (data.getProperties() != null) {
            throw new AssertionError("must not set data properties before calling init");
        }
        initTelemetry(telemetry, data, exceptionTelemetryName, "ExceptionData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    // must be called before setting any telemetry tags or data properties
    //
    // telemetry tags will be non-null after this call
    // data properties may or may not be non-null after this call
    public void initMessageTelemetry(TelemetryItem telemetry, MessageData data) {
        if (telemetry.getTags() != null) {
            throw new AssertionError("must not set telemetry tags before calling init");
        }
        if (data.getProperties() != null) {
            throw new AssertionError("must not set data properties before calling init");
        }
        initTelemetry(telemetry, data, messageTelemetryName, "MessageData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    // must be called before setting any telemetry tags or data properties
    //
    // telemetry tags will be non-null after this call
    // data properties may or may not be non-null after this call
    // FIXME (trask) rename MetricsData to MetricData to match the telemetryName and baseType?
    public void initMetricTelemetry(TelemetryItem telemetry, MetricsData data, MetricDataPoint point) {
        if (telemetry.getTags() != null) {
            throw new AssertionError("must not set telemetry tags before calling init");
        }
        if (data.getProperties() != null) {
            throw new AssertionError("must not set data properties before calling init");
        }
        initTelemetry(telemetry, data, metricTelemetryName, "MetricData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
        data.setMetrics(singletonList(point));
    }

    // must be called before setting any telemetry tags or data properties
    //
    // telemetry tags will be non-null after this call
    // data properties may or may not be non-null after this call
    public void initPageViewTelemetry(TelemetryItem telemetry, PageViewData data) {
        if (telemetry.getTags() != null) {
            throw new AssertionError("must not set telemetry tags before calling init");
        }
        if (data.getProperties() != null) {
            throw new AssertionError("must not set data properties before calling init");
        }
        initTelemetry(telemetry, data, pageViewTelemetryName, "PageViewData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    // must be called before setting any telemetry tags or data properties
    //
    // telemetry tags will be non-null after this call
    // data properties may or may not be non-null after this call
    public void initRemoteDependencyTelemetry(TelemetryItem telemetry, RemoteDependencyData data) {
        if (telemetry.getTags() != null) {
            throw new AssertionError("must not set telemetry tags before calling init");
        }
        if (data.getProperties() != null) {
            throw new AssertionError("must not set data properties before calling init");
        }
        initTelemetry(telemetry, data, remoteDependencyTelemetryName, "RemoteDependencyData");
        if (!globalProperties.isEmpty()) {
            data.setProperties(new HashMap<>(globalProperties));
        }
    }

    // must be called before setting any telemetry tags or data properties
    //
    // telemetry tags will be non-null after this call
    // data properties may or may not be non-null after this call
    public void initRequestTelemetry(TelemetryItem telemetry, RequestData data) {
        if (telemetry.getTags() != null) {
            throw new AssertionError("must not set telemetry tags before calling init");
        }
        if (data.getProperties() != null) {
            throw new AssertionError("must not set data properties before calling init");
        }
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
