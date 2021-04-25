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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.azure.monitor.opentelemetry.exporter.implementation.ApplicationInsightsClientImpl;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;

// Created by gupele
/**
 * Create an instance of this class to send telemetry to Azure Application Insights.
 * General overview https://docs.microsoft.com/azure/application-insights/app-insights-api-custom-events-metrics
 */
public class TelemetryClient {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);

    private static final AtomicLong generateCounter = new AtomicLong(0);

    // globalTags contain:
    // * cloud role name
    // * cloud role instance
    // * sdk version
    // * application version (if provided in customDimensions)
    private final Map<String, String> globalTags;
    // contains customDimensions from json configuration
    private final Map<String, String> globalProperties;

    private final TelemetryConfiguration configuration;

    public TelemetryClient() {
        this(TelemetryConfiguration.getActive());
    }

    public TelemetryClient(TelemetryConfiguration configuration) {

        StringSubstitutor substitutor = new StringSubstitutor(System.getenv());
        Map<String, String> globalProperties = new HashMap<>();
        Map<String, String> globalTags = new HashMap<>();
        for (Map.Entry<String, String> entry : configuration.getCustomDimensions().entrySet()) {
            String key = entry.getKey();
            if (key.equals("service.version")) {
                globalTags.put(ContextTagKeys.AI_APPLICATION_VER.toString(), substitutor.replace(entry.getValue()));
            } else {
                globalProperties.put(key, substitutor.replace(entry.getValue()));
            }
        }

        globalTags.put(ContextTagKeys.AI_CLOUD_ROLE.toString(), configuration.getRoleName());
        globalTags.put(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString(), configuration.getRoleInstance());
        globalTags.put(ContextTagKeys.AI_INTERNAL_SDK_VERSION.toString(), PropertyHelper.getQualifiedSdkVersionString());

        this.globalProperties = globalProperties;
        this.globalTags = globalTags;

        this.configuration = configuration;
    }

    public void track(TelemetryItem telemetry) {

        if (generateCounter.incrementAndGet() % 10000 == 0) {
            logger.debug("Total events generated till now {}", generateCounter.get());
        }

        if (telemetry == null) {
            // TODO (trask) remove this after confident no code paths hit this
            throw new IllegalArgumentException("telemetry item cannot be null");
        }

        if (telemetry.getTime() == null) {
            // TODO (trask) remove this after confident no code paths hit this
            throw new IllegalArgumentException("telemetry item is missing time");
        }

        // do not overwrite if the user has explicitly set the instrumentation key
        // (either via 2.x SDK or ai.preview.instrumentation_key span attribute)
        if (Strings.isNullOrEmpty(telemetry.getInstrumentationKey())) {
            // TODO (trask) make sure instrumentation key is always set before calling track()
            // FIXME (trask) this used to be optimized by passing in normalized instrumentation key as well
            telemetry.setInstrumentationKey(configuration.getInstrumentationKey());
        }

        // globalTags contain:
        // * cloud role name
        // * cloud role instance
        // * sdk version
        // * component version
        // do not overwrite if the user has explicitly set the cloud role name, cloud role instance,
        // or application version (either via 2.x SDK, ai.preview.service_name, ai.preview.service_instance_id,
        // or ai.preview.service_version span attributes)
        for (Map.Entry<String, String> entry : globalTags.entrySet()) {
            String key = entry.getKey();
            // only overwrite ai.internal.* tags, e.g. sdk version
            if (key.startsWith("ai.internal.") || !telemetry.getTags().containsKey(key)) {
                telemetry.getTags().put(key, entry.getValue());
            }
        }

        // populated from json configuration customDimensions
        TelemetryUtil.getProperties(telemetry.getData().getBaseData()).putAll(globalProperties);

        QuickPulseDataCollector.INSTANCE.add(telemetry);

        ApplicationInsightsClientImpl channel = configuration.getChannel();

        try {
            // FIXME (trask) do something with return value, for flushing / shutdown purpose
            channel.trackAsync(singletonList(telemetry));
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                logger.error("Exception while sending telemetry: '{}'",t.toString());            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }

        TelemetryObservers.INSTANCE.getObservers().forEach(consumer -> consumer.accept(telemetry));
    }

    /**
     * Flushes possible pending Telemetries in the channel. Not required for a continuously-running server application.
     */
    public void flush() {
        // FIXME (trask)
        // getChannel().flush();
    }

    public void shutdown(long timeout, TimeUnit timeUnit) throws InterruptedException {
        // FIXME (trask)
        // getChannel().shutdown(timeout, timeUnit);
    }
}
