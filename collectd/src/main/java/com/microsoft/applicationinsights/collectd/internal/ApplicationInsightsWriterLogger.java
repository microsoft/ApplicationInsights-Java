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

package com.microsoft.applicationinsights.collectd.internal;

import org.collectd.api.Collectd;

/**
 * Created by yonisha on 5/4/2015.
 *
 * This is class is mainly for testability of the writer.
 */
public class ApplicationInsightsWriterLogger {

    private static final String MESSAGE_PREFIX = "[Application Insights Java Plugin] ";
    private boolean enabled;

    /**
     * Constructs new instance of @ApplicationInsightsWriterLogger
     */
    public ApplicationInsightsWriterLogger() {
        this.enabled = true;
    }

    /**
     * Constructs new instance of @ApplicationInsightsWriterLogger
     * @param enabled True to enable the plugin, false otherwise.
     */
    public ApplicationInsightsWriterLogger(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Log message in debug level.
     * @param message The message to log.
     */
    public void logDebug(String message) {
        if (!this.enabled) {
            return;
        }

        Collectd.logDebug(MESSAGE_PREFIX + message);
    }

    /**
     * Log message in info level.
     * @param message The message to log.
     */
    public void logInfo(String message) {
        if (!this.enabled) {
            return;
        }

        Collectd.logInfo(MESSAGE_PREFIX + message);
    }

    /**
     * Log message in warning level.
     * @param message The message to log.
     */
    public void logWarning(String message) {
        if (!this.enabled) {
            return;
        }

        Collectd.logWarning(MESSAGE_PREFIX + message);
    }

    /**
     * Log message in error level.
     * @param message The message to log.
     */
    public void logError(String message) {
        if (!this.enabled) {
            return;
        }

        Collectd.logError(MESSAGE_PREFIX + message);
    }
}
