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

import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggerOutputType;
import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggingLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

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
     * Logger properties.
     */
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

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public static class Logger {
        /**
         * Type of application insights logger.
         */
        private LoggerOutputType type = LoggerOutputType.CONSOLE;
        /**
         * Minimal level of application insights logger.
         */
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
