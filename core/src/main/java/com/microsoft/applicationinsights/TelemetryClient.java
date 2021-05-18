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

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.serializer.*;
import com.azure.monitor.opentelemetry.exporter.implementation.ApplicationInsightsClientImplBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.authentication.AadAuthentication;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.TelemetryClientInitializer;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString;
import com.microsoft.applicationinsights.internal.config.connection.EndpointProvider;
import com.microsoft.applicationinsights.internal.config.connection.InvalidConnectionStringException;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector;
import com.microsoft.applicationinsights.internal.util.CollectionTypeJsonSerializer;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.apache.commons.text.StringSubstitutor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;

public class TelemetryClient {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);

    // Synchronization for instance initialization
    private static final Object s_lock = new Object();
    private static volatile TelemetryClient active;

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

    private final Object channelInitLock = new Object();
    private volatile @Nullable BatchSpanProcessor channel;

    // only used by tests
    public TelemetryClient() {
        this(new HashMap<>());
    }

    public TelemetryClient(Map<String, String> customDimensions) {
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
    public static TelemetryClient initActive(Map<String, String> customDimensions, ApplicationInsightsXmlConfiguration applicationInsightsConfig) {
        if (active != null) {
            throw new IllegalStateException("Already initialized");
        }
        if (active == null) {
            synchronized (s_lock) {
                if (active == null) {
                    TelemetryClient active = new TelemetryClient(customDimensions);
                    TelemetryClientInitializer.INSTANCE.initialize(active, applicationInsightsConfig);
                    TelemetryClient.active = active;
                }
            }
        }
        return active;
    }

    // FIXME (trask) inject TelemetryClient in tests instead of using global
    @Deprecated
    public static void resetForTesting() {
        active = null;
    }

    public void trackAsync(List<TelemetryItem> telemetryItems) {
        for (TelemetryItem telemetry : telemetryItems) {
            trackAsync(telemetry);
        }
    }

    public void trackAsync(TelemetryItem telemetry) {
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

        getChannel().trackAsync(telemetry);
    }

    public BatchSpanProcessor getChannel() {
        if (channel == null) {
            synchronized (channelInitLock) {
                if (channel == null) {
                    channel = createChannel();
                }
            }
        }
        return channel;
    }

    private BatchSpanProcessor createChannel() {
        ApplicationInsightsClientImplBuilder restServiceClientBuilder = new ApplicationInsightsClientImplBuilder();
        restServiceClientBuilder.serializerAdapter(new JacksonJsonAdapter());
        URI endpoint = endpointProvider.getIngestionEndpoint();
        try {
            URI hostOnly = new URI(endpoint.getScheme(), endpoint.getUserInfo(), endpoint.getHost(), endpoint.getPort(), null, null, null);
            restServiceClientBuilder.host(hostOnly.toString());
        } catch (URISyntaxException e) {
            // TODO (trask) revisit what's an appropriate action here?
            logger.error(e.getMessage(), e);
        }
        // handle AAD authentication
        // TODO handle authentication exceptions
        HttpPipelinePolicy authenticationPolicy = AadAuthentication.getInstance().getAuthenticationPolicy();
        if(authenticationPolicy != null) {
            restServiceClientBuilder.addPolicy(authenticationPolicy);
        }

        return BatchSpanProcessor.builder(restServiceClientBuilder.buildClient()).build();
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

    public void flush() {
        // FIXME (trask)
    }

    public void shutdown(int time, TimeUnit unit) throws InterruptedException {
        // FIXME (trask)
    }

    // need to implement our own SerializerAdapter for the agent in order to avoid instantiating any xml classes
    // because wildfly sets system property:
    //   javax.xml.stream.XMLInputFactory=__redirected.__XMLInputFactory
    // and that class is available in the system class loader, but not in the agent class loader
    // because the agent class loader parents the bootstrap class loader directly
    private static class JacksonJsonAdapter implements SerializerAdapter {

        private final ObjectMapper mapper;

        private JacksonJsonAdapter() {
            mapper = JsonMapper.builder().build();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            // Customize serializer to use NDJSON
            SimpleModule ndjsonModule = new SimpleModule("Ndjson List Serializer");
            ndjsonModule.setSerializers(new CollectionTypeJsonSerializer());
            mapper.registerModule(ndjsonModule);
        }

        @Override
        public String serialize(Object object, SerializerEncoding encoding) throws IOException {
            if (object == null) {
                return null;
            }
            return mapper.writeValueAsString(object);
        }

        @Override
        public String serializeRaw(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String serializeList(List<?> list, CollectionFormat format) {
            return serializeIterable(list, format);
        }

        @Override
        public <T> T deserialize(String value, Type type, SerializerEncoding encoding) {
            // FIXME (trask)
            return null;
        }

        @Override
        public <T> T deserialize(HttpHeaders headers, Type type) {
            // FIXME (trask)
            return null;
        }
    }
}
