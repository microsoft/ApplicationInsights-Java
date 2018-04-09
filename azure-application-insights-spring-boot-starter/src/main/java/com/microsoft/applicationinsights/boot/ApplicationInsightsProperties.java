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

package com.microsoft.applicationinsights.boot;

import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionFileSystemOutput;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput;
import com.microsoft.applicationinsights.internal.channel.samplingV2.FixedRateSamplingTelemetryProcessor;
import com.microsoft.applicationinsights.internal.channel.samplingV2.TelemetryType;
import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggerOutputType;
import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggingLevel;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterContainer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebOperationIdTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebOperationNameTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebSessionTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebUserAgentTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebUserTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.modules.WebRequestTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebSessionTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebUserTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.internal.perfcounter.WebPerformanceCounterModule;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring application insights.
 *
 * @author Arthur Gavlyukovskiy
 */
@ConfigurationProperties("azure.application-insights")
public class ApplicationInsightsProperties {

    /**
     * Enables application insights auto-configuration.
     */
    private boolean enabled = true;
    /**
     * Instrumentation key from Azure Portal.
     */
    private String instrumentationKey;
    /**
     * Telemetry transmission channel configuration.
     */
    private Channel channel = new Channel();
    /**
     * Built in telemetry processors configuration.
     */
    private TelemetryProcessor telemetryProcessor = new TelemetryProcessor();
    /**
     * Web plugins settings.
     */
    private Web web = new Web();
    /**
     * Quick Pulse settings.
     */
    private QuickPulse quickPulse = new QuickPulse();
    /**
     * Logger properties.
     */
    private Logger logger = new Logger();

    /**
     * Performance Counter Container Properties
     */

    private PerformanceCounter performanceCounter = new PerformanceCounter();

    /**
     * Jmx Counter container
     */
    private Jmx jmx = new Jmx();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    public void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public TelemetryProcessor getTelemetryProcessor() {
        return telemetryProcessor;
    }

    public void setTelemetryProcessor(TelemetryProcessor telemetryProcessor) {
        this.telemetryProcessor = telemetryProcessor;
    }

    public Web getWeb() {
        return web;
    }

    public void setWeb(Web web) {
        this.web = web;
    }

    public QuickPulse getQuickPulse() {
        return quickPulse;
    }

    public void setQuickPulse(QuickPulse quickPulse) {
        this.quickPulse = quickPulse;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;

    }

    public PerformanceCounter getPerformanceCounter() {
        return performanceCounter;
    }

    public void setPerformanceCounter(
        PerformanceCounter performanceCounter) {
        this.performanceCounter = performanceCounter;
    }

    public Jmx getJmx() {
        return jmx;
    }

    public void setJmx(Jmx jmx) {
        this.jmx = jmx;
    }

    static class Channel {
        /**
         * Configuration of {@link InProcessTelemetryChannel}.
         */
        private InProcess inProcess = new InProcess();

        public InProcess getInProcess() {
            return inProcess;
        }

        public void setInProcess(InProcess inProcess) {
            this.inProcess = inProcess;
        }

        static class InProcess {
            /**
             * Enables developer mode, all telemetry will be sent immediately without batching.
             * Significantly affects performance and should be used only in developer environment.
             */
            private boolean developerMode = false;
            /**
             * Endpoint address.
             */
            private String endpointAddress = TransmissionNetworkOutput.DEFAULT_SERVER_URI;
            /**
             * Maximum count of telemetries that will be batched before sending. Must be between 1 and 1000.
             */
            private int maxTelemetryBufferCapacity = InProcessTelemetryChannel.DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY;
            /**
             * Interval to send telemetry. Must be between 1 and 300.
             */
            private int flushIntervalInSeconds = InProcessTelemetryChannel.DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS;
            /**
             * Size of disk space that Application Insights can use to store telemetry in case of network outage. Must be between 1 and 1000.
             */
            private int maxTransmissionStorageFilesCapacityInMb = TransmissionFileSystemOutput.DEFAULT_CAPACITY_MEGABYTES;
            /**
             * Enables throttling on sending telemetry data.
             */
            private boolean throttling = true;

            /**
             * Sets the size of maximum instant retries without delay
             */
            private int maxInstantRetry = InProcessTelemetryChannel.DEFAULT_MAX_INSTANT_RETRY;

            public boolean isDeveloperMode() {
                return developerMode;
            }

            public void setDeveloperMode(boolean developerMode) {
                this.developerMode = developerMode;
            }

            public int getMaxInstantRetry() {
                return maxInstantRetry;
            }

            public void setMaxInstantRetry(int maxInstantRetry) {
                this.maxInstantRetry = maxInstantRetry;
            }

            public String getEndpointAddress() {
                return endpointAddress;
            }

            public void setEndpointAddress(String endpointAddress) {
                this.endpointAddress = endpointAddress;
            }

            public int getMaxTelemetryBufferCapacity() {
                return maxTelemetryBufferCapacity;
            }

            public void setMaxTelemetryBufferCapacity(int maxTelemetryBufferCapacity) {
                this.maxTelemetryBufferCapacity = maxTelemetryBufferCapacity;
            }

            public int getFlushIntervalInSeconds() {
                return flushIntervalInSeconds;
            }

            public void setFlushIntervalInSeconds(int flushIntervalInSeconds) {
                this.flushIntervalInSeconds = flushIntervalInSeconds;
            }

            public int getMaxTransmissionStorageFilesCapacityInMb() {
                return maxTransmissionStorageFilesCapacityInMb;
            }

            public void setMaxTransmissionStorageFilesCapacityInMb(int maxTransmissionStorageFilesCapacityInMb) {
                this.maxTransmissionStorageFilesCapacityInMb = maxTransmissionStorageFilesCapacityInMb;
            }

            public boolean isThrottling() {
                return throttling;
            }

            public void setThrottling(boolean throttling) {
                this.throttling = throttling;
            }
        }
    }

    static class TelemetryProcessor {

        /**
         * Configuration of {@link FixedRateSamplingTelemetryProcessor}.
         */
        private Sampling sampling = new Sampling();

        public Sampling getSampling() {
            return sampling;
        }

        public void setSampling(Sampling sampling) {
            this.sampling = sampling;
        }

        static class Sampling {
            /**
             * Percent of telemetry events that will be sent to Application Insights. Percentage must be close to 100/N where N is an integer.
             * E.g. 50 (=100/2), 33.33 (=100/3), 25 (=100/4), 20, 1 (=100/100), 0.1 (=100/1000).
             */
            private double percentage = FixedRateSamplingTelemetryProcessor.DEFAULT_SAMPLING_PERCENTAGE;
            /**
             * If set only telemetry of specified types will be included.
             */
            private List<TelemetryType> include = new ArrayList<>();
            /**
             * If set telemetry of specified type will be excluded.
             */
            private List<TelemetryType> exclude = new ArrayList<>();

            public double getPercentage() {
                return percentage;
            }

            public void setPercentage(double percentage) {
                this.percentage = percentage;
            }

            public List<TelemetryType> getInclude() {
                return include;
            }

            public void setInclude(List<TelemetryType> include) {
                this.include = include;
            }

            public List<TelemetryType> getExclude() {
                return exclude;
            }

            public void setExclude(List<TelemetryType> exclude) {
                this.exclude = exclude;
            }
        }
    }

    static class Web {
        /**
         * Enables Web telemetry modules.
         *
         * Implicitly affects modules:
         *  - {@link WebRequestTrackingTelemetryModule}
         *  - {@link WebSessionTrackingTelemetryModule}
         *  - {@link WebUserTrackingTelemetryModule}
         *  - {@link WebPerformanceCounterModule}
         *  - {@link WebOperationIdTelemetryInitializer}
         *  - {@link WebOperationNameTelemetryInitializer}
         *  - {@link WebSessionTelemetryInitializer}
         *  - {@link WebUserTelemetryInitializer}
         *  - {@link WebUserAgentTelemetryInitializer}
         *
         *  False means that all those modules will be disabled
         *  regardless of the enabled property of concrete module.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class QuickPulse {
        /**
         * Enables Quick Pulse integration.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    static class Logger {
        /**
         * Type of application insights logger.
         */
        private LoggerOutputType type = LoggerOutputType.CONSOLE;
        /**
         * Minimal level of application insights logger.
         */
        private LoggingLevel level = LoggingLevel.OFF;

        public LoggerOutputType getType() {
            return type;
        }

        public void setType(LoggerOutputType type) {
            this.type = type;
        }

        public LoggingLevel getLevel() {
            return level;
        }

        public void setLevel(LoggingLevel level) {
            this.level = level;
        }
    }

    static class PerformanceCounter {

        /**
         * Default collection frequency of performance counters
         */
        private long collectionFrequencyInSeconds = PerformanceCounterContainer.DEFAULT_COLLECTION_FREQUENCY_IN_SEC;

        public long getCollectionFrequencyInSeconds() {
            return collectionFrequencyInSeconds;
        }

        public void setCollectionFrequencyInSeconds(long collectionFrequencyInSeconds) {
            this.collectionFrequencyInSeconds = collectionFrequencyInSeconds;
        }
    }

    static class Jmx {

        /**
         * List of JMX counters
         */
        List<String> jmxCounters = new ArrayList<>();

        public List<String> getJmxCounters() {
            return jmxCounters;
        }

        public void setJmxCounters(List<String> jmxCounters) {
            this.jmxCounters = jmxCounters;
        }
    }
}
