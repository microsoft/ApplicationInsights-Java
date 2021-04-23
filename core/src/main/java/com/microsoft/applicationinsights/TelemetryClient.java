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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.context.ContextTagKeys;
import com.microsoft.applicationinsights.extensibility.context.InternalContext;
import com.microsoft.applicationinsights.extensibility.initializer.TelemetryObservers;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Created by gupele
/**
 * Create an instance of this class to send telemetry to Azure Application Insights.
 * General overview https://docs.microsoft.com/azure/application-insights/app-insights-api-custom-events-metrics
 */
public class TelemetryClient {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);

    private final TelemetryConfiguration configuration;
    private volatile TelemetryContext context;
    private TelemetryChannel channel;

    private static final Object TELEMETRY_CONTEXT_LOCK = new Object();

    private static final AtomicLong generateCounter = new AtomicLong(0);
    /**
     * Initializes a new instance of the TelemetryClient class. Send telemetry with the specified configuration.
     * @param configuration The configuration this instance will work with.
     */
    public TelemetryClient(TelemetryConfiguration configuration) {
        if (configuration == null) {
            configuration = TelemetryConfiguration.getActive();
        }

        this.configuration = configuration;
    }

    /**
     * Initializes a new instance of the TelemetryClient class, configured from the active configuration.
     */
    public TelemetryClient() {
        this(TelemetryConfiguration.getActive());
    }

    /**
     * Gets the current context that is used to augment telemetry you send.
     * @return A telemetry context used for all records. Changes to it will impact all future telemetry in this
     * application session.
     */
    public TelemetryContext getContext() {
        if (context == null || (context.getInstrumentationKey() != null &&  !context.getInstrumentationKey().equals(configuration.getInstrumentationKey()))) {
            // lock and recheck there is still no initialized context. If so, create one.
            synchronized (TELEMETRY_CONTEXT_LOCK) {
                if (context==null || (context.getInstrumentationKey() != null && !context.getInstrumentationKey().equals(configuration.getInstrumentationKey()))) {
                    context = createInitializedContext();
                }
            }
        }

        return context;
    }

    /**
     * Checks whether tracking is enabled.
     * @return 'true' if tracking is disabled, 'false' otherwise.
     */
    public boolean isDisabled() {
        return Strings.isNullOrEmpty(configuration.getInstrumentationKey()) && Strings.isNullOrEmpty(getContext().getInstrumentationKey());
    }

    /**
     * This method is part of the Application Insights infrastructure. Do not call it directly.
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} instance.
     */
    public void track(Telemetry telemetry) {

        if (generateCounter.incrementAndGet() % 10000 == 0) {
            logger.debug("Total events generated till now {}", generateCounter.get());
        }

        if (telemetry == null) {
            throw new IllegalArgumentException("telemetry item cannot be null");
        }

        if (isDisabled()) {
            return;
        }

        if (telemetry.getTimestamp() == null) {
            telemetry.setTimestamp(new Date());
        }

        TelemetryContext context = telemetry.getContext();

        // do not overwrite if the user has explicitly set the instrumentation key
        // (either via 2.x SDK or ai.preview.instrumentation_key span attribute)
        if (Strings.isNullOrEmpty(context.getInstrumentationKey())) {
            context.setInstrumentationKey(getContext().getInstrumentationKey(), getContext().getNormalizedInstrumentationKey());
            logger.debug("##################################### global context instrumentation key: {}", context.getInstrumentationKey());
        } else {
            logger.debug("##################################### StatsbeatTelemetry instrumentation key: {}", context.getInstrumentationKey());
        }

        // the TelemetryClient's base context contains tags:
        // * cloud role name
        // * cloud role instance
        // * sdk version
        // * component version
        // do not overwrite if the user has explicitly set the cloud role name, cloud role instance,
        // or application version (either via 2.x SDK, ai.preview.service_name, ai.preview.service_instance_id,
        // or ai.preview.service_version span attributes)
        for (Map.Entry<String, String> entry : getContext().getTags().entrySet()) {
            String key = entry.getKey();
            // only overwrite ai.internal.* tags, e.g. sdk version
            if (key.startsWith("ai.internal.") || !context.getTags().containsKey(key)) {
                context.getTags().put(key, entry.getValue());
            }
        }

        // the TelemetryClient's base context contains properties:
        // * "customDimensions" provided by json configuration
        context.getProperties().putAll(getContext().getProperties());

        try {
            QuickPulseDataCollector.INSTANCE.add(telemetry);
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
        }

        try {
            getChannel().send(telemetry);
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

        TelemetryObservers.INSTANCE.getObservers().forEach(consumer -> consumer.consume(telemetry));
    }

    /**
     * Flushes possible pending Telemetries in the channel. Not required for a continuously-running server application.
     */
    public void flush() {
        getChannel().flush();
    }

    public void shutdown(long timeout, TimeUnit timeUnit) throws InterruptedException {
        getChannel().shutdown(timeout, timeUnit);
    }

    /**
     * Gets the channel used by the client.
     */
    TelemetryChannel getChannel() {
        if (this.channel == null) {
            this.channel = configuration.getChannel();
        }

        return this.channel;
    }

    private TelemetryContext createInitializedContext() {
        TelemetryContext ctx = new TelemetryContext();
        ctx.setInstrumentationKey(configuration.getInstrumentationKey());
        String roleName = configuration.getRoleName();
        if (StringUtils.isNotEmpty(roleName)) {
            ctx.getCloud().setRole(roleName);
        }
        String roleInstance = configuration.getRoleInstance();
        if (StringUtils.isNotEmpty(roleInstance)) {
            ctx.getCloud().setRoleInstance(roleInstance);
        }
        for (ContextInitializer init : configuration.getContextInitializers()) {
            if (init == null) { // since collection reference is exposed, we need a null check here
                logger.warn("Found null ContextInitializer in configuration. Skipping...");
                continue;
            }

            try {
                init.initialize(ctx);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                try {
                    if (logger.isErrorEnabled()) {
                        logger.error("Exception in context initializer, {}: {}", init.getClass().getSimpleName(), ExceptionUtils.getStackTrace(t));
                    }
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t2) {
                    // chomp
                }
            }
        }

        // Set the nodeName for billing purpose if it does not already exist
        InternalContext internal = ctx.getInternal();
        if (CommonUtils.isNullOrEmpty(internal.getNodeName())) {
            String host = CommonUtils.getHostName();
            if (!CommonUtils.isNullOrEmpty(host)) {
                internal.setNodeName(host);
            }
        }
        return ctx;
    }
}
