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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.microsoft.applicationinsights.agent.bootstrap.customExceptions.FriendlyException;

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
    public SelfDiagnostics selfDiagnostics = new SelfDiagnostics();
    public PreviewConfiguration preview = new PreviewConfiguration();

    // this is just here to detect if using old format in order to give a helpful error message
    public Map<String, Object> instrumentationSettings;

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

        public boolean developerMode;
        public List<ProcessorConfig> processors = new ArrayList<>();
        public boolean openTelemetryApiSupport;
    }

    public static class SelfDiagnostics {

        public String level = "info";
        public String destination = "file+console";
        public DestinationFile file = new DestinationFile();
    }

    public static class DestinationFile {

        public String path = "applicationinsights.log"; // relative to the directory where agent jar is located
        public int maxSizeMb = 5;
        public int maxHistory = 1;
    }

    public static class ProcessorConfig {
        public ProcessorType type;
        public String processorName;
        public ProcessorIncludeExclude include;
        public ProcessorIncludeExclude exclude;
        public List<ProcessorAction> actions; // specific for processor type "attributes"
        public NameConfig name; // specific for processor types "log" and "span"

        private static void isValidRegex(String value) throws FriendlyException {
            try {
                Pattern.compile(value);
            } catch (PatternSyntaxException exception) {
                throw new FriendlyException("Telemetry processor configuration does not have valid regex:" + value,
                                "Please provide a valid regex in the telemetry processors configuration. " +
                                "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
            }
        }

        public void validate() throws FriendlyException {
            if (type == null) {
                throw new FriendlyException("Telemetry processor configuration has a processor with no type!!!",
                                "Please provide a type in the telemetry processors configuration. " +
                                "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
            }
            if (include != null ) {
                include.validate(type);
            }
            if (exclude != null ) {
               exclude.validate(type);
            }
            validateAttributeProcessorConfig();
            validateLogOrSpanProcessorConfig();
        }

        public void validateAttributeProcessorConfig() throws FriendlyException {
            if (type == ProcessorType.attribute) {
                if (actions == null || actions.isEmpty()) {
                    throw new FriendlyException("Telemetry processor configuration has invalid attribute processor configuration with empty actions!!!",
                                    "Please provide at least one action in the attribute processors configuration. " +
                                    "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
                }
                for (ProcessorAction action : actions) {
                    action.validate();
                }
            }
        }

        public void validateLogOrSpanProcessorConfig() throws FriendlyException {
            if (type == ProcessorType.log || type == ProcessorType.span) {
                if (name == null) {
                    throw new FriendlyException("Telemetry processor configuration has invalid span/log processor configuration with empty name object!!!",
                                    "Please provide name in the span/log processor configuration. " +
                                    "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
                }
                name.validate();
            }
        }
    }

    public static class NameConfig {
        public List<String> fromAttributes;
        public ToAttributeConfig toAttributes;
        public String separator;

        public void validate() throws FriendlyException {
            if (fromAttributes == null && toAttributes == null) {
                throw new FriendlyException("Telemetry processor configuration has invalid name object with no fromAttributes or no toAttributes!!!",
                                "Please provide at least one of fromAttributes or toAttributes in the processor configuration. " +
                                "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
            }
            if (toAttributes != null)  toAttributes.validate();
        }
    }

    public static class ToAttributeConfig {
        public List<String> rules;

        public void validate() throws FriendlyException {
            if(rules==null || rules.isEmpty()) {
                throw new FriendlyException("Telemetry processor configuration has invalid toAttribute value with no rules!!!",
                                "Please provide at least one rule under the toAttribute section of processor configuration. " +
                                "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
            }
            for (String rule : rules) {
                ProcessorConfig.isValidRegex(rule);
            }
        }
    }


    public static class ProcessorIncludeExclude {
        public ProcessorMatchType matchType;
        public List<String> spanNames;
        public List<String> logNames;
        public List<ProcessorAttribute> attributes;

        public void validate (ProcessorType processorType) throws FriendlyException {
            if (this.matchType == null) {
                throw new FriendlyException("Telemetry processor configuration has invalid include/exclude value with no matchType!!!",
                                "Please provide matchType under the include/exclude section of processor configuration. " +
                                "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
            }
            if (this.attributes != null) {
                for (ProcessorAttribute attribute : this.attributes) {
                    if (attribute.key == null || attribute.key.isEmpty()) {
                        throw new FriendlyException("Telemetry processor configuration has invalid include/exclude value with attribute which has empty key!!!",
                                        "Please provide valid key with value under the include/exclude's attribute section of processor configuration. " +
                                        "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
                    }
                    if (this.matchType == ProcessorMatchType.regexp && attribute.value != null) {
                        ProcessorConfig.isValidRegex(attribute.value);
                    }
                }
            }

            switch(processorType) {
                case attribute: validAttributeProcessorIncludeExclude(); break;
                case log : validateLogProcessorIncludeExclude(); break;
                case span: validateSpanProcessorIncludeExclude();
                default: break;
            }

        }

        private void validAttributeProcessorIncludeExclude() throws FriendlyException {
            if (spanNames == null && attributes == null) {
                throw new FriendlyException("Telemetry processor configuration has invalid include/exclude value with no spanNames or no attributes!!!",
                                "Please provide at least one of spanNames or attributes under the include/exclude section of processor configuration. " +
                                "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
            }
            if (spanNames != null && matchType == ProcessorMatchType.regexp) {
                for (String spanName : spanNames) {
                    ProcessorConfig.isValidRegex(spanName);
                }
            }
        }

        private void validateLogProcessorIncludeExclude() throws FriendlyException {
            if (logNames == null && attributes == null) {
                throw new FriendlyException("Telemetry processor configuration has invalid include/exclude value with no logNames or no attributes!!!",
                                "Please provide at least one of logNames or attributes under the include/exclude section of processor configuration. " +
                                "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
            }
            if (logNames != null && matchType == ProcessorMatchType.regexp) {
                for (String logName : logNames) {
                    ProcessorConfig.isValidRegex(logName);
                }
            }
        }

        private void validateSpanProcessorIncludeExclude() throws FriendlyException {
                if (spanNames == null && attributes == null) {
                    throw new FriendlyException("Telemetry processor configuration has invalid include/exclude value with no spanNames or no attributes!!!",
                                    "Please provide at least one of spanNames or attributes under the include/exclude section of processor configuration. " +
                                    "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
                }
                if (spanNames != null && matchType == ProcessorMatchType.regexp) {
                    for (String spanName : spanNames) {
                        ProcessorConfig.isValidRegex(spanName);
                    }
                }
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

        public void validate() throws FriendlyException {

                if (this.key == null || this.key.isEmpty()) {
                    throw new FriendlyException("Telemetry processor configuration has invalid action with empty key!!!",
                                    "Please provide a valid key with value under each action section of processor configuration. " +
                                    "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
                }
                if (this.action == null) {
                    throw new FriendlyException("Telemetry processor configuration has invalid config with empty action!!!",
                                    "Please provide a valid action. Telemetry processors cannot have empty or no actions. " +
                                    "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
                }
                if (this.action == ProcessorActionType.insert || this.action == ProcessorActionType.update) {
                    if(this.value == null && this.fromAttribute == null) {
                        throw new FriendlyException("Telemetry processor configuration has invalid action with empty value or empty fromAttribute!!!",
                                        "Please provide a valid action with value under each action section of processor configuration. " +
                                        "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557");
                    }
                }
        }
    }

    // transient so that Moshi will ignore when binding from json
    public transient Path configPath;

    // transient so that Moshi will ignore when binding from json
    public transient long lastModifiedTime;

}
