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

package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InstrumentationSettings {

    public String connectionString;
    public String instrumentationKey;
    public PreviewConfiguration preview = new PreviewConfiguration();

    public static class PreviewConfiguration {

        public String roleName;
        public String roleInstance;
        public SelfDiagnostics selfDiagnostics = new SelfDiagnostics();
        public Sampling sampling = new Sampling();
        public Heartbeat heartbeat = new Heartbeat();
        public HttpProxy httpProxy = new HttpProxy();
        public LiveMetrics liveMetrics = new LiveMetrics();
        public boolean developerMode;

        public List<JmxMetric> jmxMetrics = Collections.emptyList();

        public Map<String, Map<String, Object>> instrumentation = Collections.emptyMap();
    }

    public static class SelfDiagnostics {

        public String destination;
        public String directory;
        public String level = "error";
        public int maxSizeMB = 10;
    }

    public static class Sampling {

        public FixedRateSampling fixedRate;
    }

    public static class FixedRateSampling {

        public Double percentage;
    }

    public static class Heartbeat {

        public boolean enabled = true;
        public long intervalSeconds = TimeUnit.MINUTES.toSeconds(15);
    }

    public static class HttpProxy {

        public String host;
        public int port = 80;
    }

    public static class LiveMetrics {

        public boolean enabled = true;
    }

    public static class JmxMetric {

        public String objectName;
        public String attribute;
        public String display;
    }
}
