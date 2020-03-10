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

package com.microsoft.applicationinsights.agent.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.microsoft.applicationinsights.internal.heartbeat.HeartBeatProviderInterface;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Configuration {

    @Nullable
    public String connectionString;
    @Nullable
    public String instrumentationKey;
    @Nullable
    public String roleName;
    @Nullable
    public String roleInstance;
    @Nullable
    public String httpProxy;
    public List<JmxMetric> jmxMetrics = Collections.emptyList();

    public ExperimentalConfiguration experimental = new ExperimentalConfiguration();

    public static class JmxMetric {

        @Nullable
        public String objectName;
        @Nullable
        public String attribute;
        @Nullable
        public String display;
    }

    public static class ExperimentalConfiguration {

        public Sampling sampling = new Sampling();
        public LiveMetrics liveMetrics = new LiveMetrics();
        public Heartbeat heartbeat = new Heartbeat();
        public Map<String, Map<String, Object>> instrumentation = Collections.emptyMap();

        public boolean debug;
        public boolean developerMode;
    }

    public static class Sampling {

        @Nullable
        public FixedRateSampling fixedRate;
    }

    public static class FixedRateSampling {

        @Nullable
        public Double percentage;
    }

    public static class LiveMetrics {

        public boolean enabled = true;
    }

    public static class Heartbeat {

        public boolean enabled = true;
        public long intervalSeconds = HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL;
    }
}
