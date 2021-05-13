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

    private final NetworkStatsbeat networkStatsbeat;
    private final AttachStatsbeat attachStatsbeat;
    private final FeatureStatsbeat featureStatsbeat;

    public static void init(TelemetryClient telemetryClient, long interval, long featureInterval) {
        instance = new StatsbeatModule(
                new NetworkStatsbeat(telemetryClient, interval),
                new AttachStatsbeat(telemetryClient, interval),
                new FeatureStatsbeat(telemetryClient, featureInterval));
    }

    public static StatsbeatModule getInstance() {
        if (instance == null) {
            throw new IllegalStateException("init must be called first");
        }
        return instance;
    }

    private StatsbeatModule(NetworkStatsbeat networkStatsbeat, AttachStatsbeat attachStatsbeat, FeatureStatsbeat featureStatsbeat) {
        this.networkStatsbeat = networkStatsbeat;
        this.attachStatsbeat = attachStatsbeat;
        this.featureStatsbeat = featureStatsbeat;
    }

    public NetworkStatsbeat getNetworkStatsbeat() { return networkStatsbeat; }

    AttachStatsbeat getAttachStatsbeat() {
        return attachStatsbeat;
    }

    FeatureStatsbeat getFeatureStatsbeat() {
        return featureStatsbeat;
    }
}
