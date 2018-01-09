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

import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggerOutputType;
import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggingLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("azure.application-insights")
public class ApplicationInsightsProperties {

    private boolean enabled = true;
    private String instrumentationKey;
    private Channel channel = new Channel();
    private QuickPulse quickPulse = new QuickPulse();
    private Logger logger = new Logger();

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

    public static class QuickPulse {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Channel {
        private InProcess inProcess = new InProcess();

        public InProcess getInProcess() {
            return inProcess;
        }

        public void setInProcess(InProcess inProcess) {
            this.inProcess = inProcess;
        }

        public static class InProcess {
            private boolean developerMode = false;
            private String endpointAddress;
            private int maxTelemetryBufferCapacity;
            private int flushIntervalInSeconds;
            private int maxTransmissionStorageFilesCapacityInMb;
            private boolean throttling = true;

            public boolean isDeveloperMode() {
                return developerMode;
            }

            public void setDeveloperMode(boolean developerMode) {
                this.developerMode = developerMode;
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

    public static class Logger {
        private LoggerOutputType type = LoggerOutputType.CONSOLE;
        private LoggingLevel level = LoggingLevel.INFO;

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
}
