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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.ConfigurationBuilder.ConfigurationException;

import static java.util.concurrent.TimeUnit.MINUTES;

public class InstrumentationSettings {

    public String connectionString;
    public PreviewConfiguration preview = new PreviewConfiguration();

    public enum ProcessorMatchType {
        //Moshi JSON builder donot allow case insensitive mapping
        strict, regexp
    }

    public enum ProcessorActionType {
        //Moshi JSON builder donot allow case insensitive mapping
        insert, update, delete, hash
    }

    public enum ProcessorType {
        //Moshi JSON builder donot allow case insensitive mapping
        attribute, log, span
    }

    public static class PreviewConfiguration {

        public String roleName;
        public String roleInstance;
        public SelfDiagnostics selfDiagnostics = new SelfDiagnostics();
        public Sampling sampling = new Sampling();
        public Heartbeat heartbeat = new Heartbeat();
        public HttpProxy httpProxy = new HttpProxy();
        public Map<String, String> resourceAttributes = new HashMap<>();
        public boolean developerMode;

        public List<JmxMetric> jmxMetrics = new ArrayList<>();
        public List<ProcessorConfig> processors = new ArrayList<>();
        public Map<String, Map<String, Object>> instrumentation = new HashMap<String, Map<String, Object>>();
    }

    public static class SelfDiagnostics {

        public String destination;
        public String directory;
        public String level = "error";
        public int maxSizeMB = 10;
    }

    public static class Sampling {

        public FixedRateSampling fixedRate = new FixedRateSampling();
    }

    public static class FixedRateSampling {

        public Double percentage;
    }

    public static class Heartbeat {

        public long intervalSeconds = MINUTES.toSeconds(15);
    }

    public static class HttpProxy {

        public String host;
        public int port = 80;
    }

    public static class JmxMetric {

        public String objectName;
        public String attribute;
        public String display;
    }

    public static class ProcessorConfig {
        public ProcessorType type;
        public String processorName;
        public ProcessorIncludeExclude include;
        public ProcessorIncludeExclude exclude;
        public List<ProcessorAction> actions; // specific for processor type "attributes"
        public NameConfig name; // specific for processor types "log" and "span"

        private static void isValidRegex(String value) {
            try {
                Pattern.compile(value);
            } catch (PatternSyntaxException exception) {
                throw new ConfigurationException("User provided processor config do not have valid regex:" + value);
            }
        }

        public boolean isValid() {
            if (type == null) return false;
            //if (processorName == null || processorName.length() == 0) return false;
            if (include != null && !include.isValid(type)) {
                return false;
            }
            if (exclude != null && !exclude.isValid(type)) {
                return false;
            }
            return hasValidAttributeProcessorConfig() && hasValidLogOrSpanProcessorConfig();
        }

        public boolean hasValidAttributeProcessorConfig() {
            if (type == ProcessorType.attribute) {
                if (actions == null || actions.isEmpty()) {
                    return false;
                }
                for (ProcessorAction action : actions) {
                    if (!action.isValid()) return false;
                }
            }
            return true;
        }

        public boolean hasValidLogOrSpanProcessorConfig() {
            if (type == ProcessorType.log || type == ProcessorType.span) {
                if (name == null) return false;
                return name.isValid();
            }
            return true;
        }
    }

    public static class NameConfig {
        public List<String> fromAttributes;
        public ToAttributeConfig toAttributes;
        public String separator;

        public boolean isValid() {
            if (fromAttributes == null && toAttributes == null) return false;
            if (toAttributes != null) return toAttributes.isValid();
            return separator == null || separator.length() != 0;
        }
    }

    public static class ToAttributeConfig {
        public List<String> rules;

        public boolean isValid() {
            if(rules==null || rules.isEmpty()) return false;
            for (String rule : rules) {
                ProcessorConfig.isValidRegex(rule);
            }
            return true;
        }
    }


    public static class ProcessorIncludeExclude {
        public ProcessorMatchType matchType;
        public List<String> spanNames;
        public List<String> logNames;
        public List<ProcessorAttribute> attributes;

        public boolean isValid(ProcessorType processorType) {
            if (this.matchType == null) return false;
            if (this.attributes != null) {
                for (ProcessorAttribute attribute : this.attributes) {
                    if (attribute.key == null) return false;
                    if (this.matchType == ProcessorMatchType.regexp && attribute.value != null) {
                        ProcessorConfig.isValidRegex(attribute.value);
                    }
                }
            }

            if (processorType == ProcessorType.attribute) {
                return hasValidAttributeProcessorIncludeExclude();
            } else if (processorType == ProcessorType.log) {
                return hasValidLogProcessorIncludeExclude();
            } else {
                return hasValidSpanProcessorIncludeExclude();
            }
        }

        private boolean hasValidAttributeProcessorIncludeExclude() {
            if (spanNames == null && attributes == null) return false;
            if (spanNames != null && matchType == ProcessorMatchType.regexp) {
                for (String spanName : spanNames) {
                    ProcessorConfig.isValidRegex(spanName);
                }
            }
            return true;
        }

        private boolean hasValidLogProcessorIncludeExclude() {
            if (logNames == null && attributes == null) return false;
            if (logNames != null && matchType == ProcessorMatchType.regexp) {
                for (String logName : logNames) {
                    ProcessorConfig.isValidRegex(logName);
                }
            }
            return true;
        }

        private boolean hasValidSpanProcessorIncludeExclude() {
            if (spanNames == null && attributes == null) return false;
            if (spanNames != null && matchType == ProcessorMatchType.regexp) {
                for (String spanName : spanNames) {
                    ProcessorConfig.isValidRegex(spanName);
                }
            }
            return true;
        }


    }

    public static class ProcessorAttribute {
        public String key;
        public String value;
    }

    public static class ProcessorAction {
        public String key;
        public ProcessorActionType action;
        public String value;
        public String fromAttribute;

        public boolean isValid() {
            if (this.key == null) return false;
            if (this.action == null) return false;
            if (this.action == ProcessorActionType.insert || this.action == ProcessorActionType.update) {
                return this.value != null || this.fromAttribute != null;
            }
            return true;
        }
    }
}
