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

package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;

public class StatsbeatModule {

    private static volatile StatsbeatModule instance;
    private static final Object lock = new Object();

    private final NetworkStatsbeat networkStatsbeat;
    private final AttachStatsbeat attachStatsbeat;
    // keeping this as field for now
    @SuppressWarnings("unused")
    private final FeatureStatsbeat featureStatsbeat;

    public static void initialize(TelemetryClient telemetryClient, long interval, long featureInterval) {
        synchronized (lock) {
            if (instance != null) {
                throw new IllegalStateException("initialize already called");
            }
            instance = new StatsbeatModule(telemetryClient, interval, featureInterval);
        }
        // will only reach here the first time, after instance has been instantiated
        new AzureMetadataService(instance.attachStatsbeat, CustomDimensions.get()).scheduleAtFixedRate(interval);
    }

    public static StatsbeatModule get() {
        return instance;
    }

    // visible for testing
    StatsbeatModule(TelemetryClient telemetryClient, long interval, long featureInterval) {
        this.networkStatsbeat = new NetworkStatsbeat(telemetryClient, interval);
        this.attachStatsbeat = new AttachStatsbeat(telemetryClient, interval);
        this.featureStatsbeat = new FeatureStatsbeat(telemetryClient, featureInterval);
    }

    public NetworkStatsbeat getNetworkStatsbeat() { return networkStatsbeat; }
}
