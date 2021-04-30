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

public final class StatsbeatModule {

    private static volatile StatsbeatModule instance;
    private NetworkStatsbeat networkStatsbeat;
    private AttachStatsbeat attachStatsbeat;
    private FeatureStatsbeat featureStatsbeat;
    private static final Object lock = new Object();

    public static StatsbeatModule getInstance() {
        synchronized (lock) {
            if (instance == null) {
                instance = new StatsbeatModule();
            }
        }
        return instance;
    }

    public void initialize(TelemetryClient telemetryClient, long interval, long featureInterval) {
        networkStatsbeat = new NetworkStatsbeat(telemetryClient, interval);
        attachStatsbeat = new AttachStatsbeat(telemetryClient, interval);
        featureStatsbeat = new FeatureStatsbeat(telemetryClient, featureInterval);
        AzureMetadataService.getInstance().initialize();
    }

    public NetworkStatsbeat getNetworkStatsbeat() {
        return networkStatsbeat;
    }

    public AttachStatsbeat getAttachStatsbeat() {
        return attachStatsbeat;
    }

    public FeatureStatsbeat getFeatureStatsbeat() {
        return featureStatsbeat;
    }
}
