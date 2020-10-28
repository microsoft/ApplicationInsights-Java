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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;

public class Configuration {

    public String connectionString;
    public Role role = new Role();
    public Map<String, String> customDimensions = new HashMap<>();
    public Sampling sampling = new Sampling();
    public List<JmxMetric> jmxMetrics = new ArrayList<>();
    public Map<String, Map<String, Object>> instrumentation = new HashMap<String, Map<String, Object>>();
    public Heartbeat heartbeat = new Heartbeat();
    public Proxy proxy = new Proxy();
    public PreviewConfiguration preview = new PreviewConfiguration();

    public static class Role {

        public String name;
        public String instance;
    }

    public static class Sampling {

        public double percentage = 100;
    }

    public static class JmxMetric {

        public String name;
        public String objectName;
        public String attribute;
    }

    public static class Heartbeat {

        public long intervalSeconds = MINUTES.toSeconds(15);
    }

    public static class Proxy {

        public String host;
        public int port = 80;
    }

    public static class PreviewConfiguration {

        public SelfDiagnostics selfDiagnostics = new SelfDiagnostics();
        public boolean developerMode;
    }

    public static class SelfDiagnostics {

        public String destination;
        public String directory;
        public String level = "error";
        public int maxSizeMB = 10;
    }

    // transient so that Moshi will ignore when binding from json
    public transient Path configPath;

    // transient so that Moshi will ignore when binding from json
    public transient long lastModifiedTime;
}
