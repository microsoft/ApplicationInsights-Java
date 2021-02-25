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
package com.microsoft.applicationinsights.serviceprofilerapi.config;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfigurationBuilder;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfigurationBuilder;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import com.microsoft.applicationinsights.alerting.config.DefaultConfigurationBuilder;
import com.microsoft.applicationinsights.profiler.ProfilerConfiguration;

/**
 * Parses the configuration from the service profiler endpoint
 */
public class AlertConfigParser {

    public static AlertingConfiguration parse(String cpuConfig, String memoryConfig, String defaultConfig, String collectionPlan) {
        return new AlertingConfiguration(
                parseFromCpu(cpuConfig),
                parseFromMemory(memoryConfig),
                parseDefaultConfiguration(defaultConfig),
                parseCollectionPlan(collectionPlan)
        );
    }

    //--single --mode immediate --immediate-profiling-duration 120  --expiration 5249143304354868449 --settings-moniker Portal_b5bd7880-7406-4058-a6f8-3ea0102706b1
    private static CollectionPlanConfiguration parseCollectionPlan(String collectionPlan) {
        if (collectionPlan == null) {
            return new CollectionPlanConfiguration(false, "", Instant.ofEpochMilli(0).atZone(ZoneOffset.UTC), 0, "");
        }

        String[] tokens = collectionPlan.split(" ");

        Map<String, ParseConfigValue<CollectionPlanConfigurationBuilder>> parsers = new HashMap<>();
        parsers.put("single", new ParseConfigValue<>(false, (config, arg) -> config.setCollectionPlanSingle(true)));
        parsers.put("mode", new ParseConfigValue<>(true, (config, arg) -> config.setMode(arg)));
        parsers.put("expiration", new ParseConfigValue<>(true, (config, arg) -> config.setExpiration(Long.parseLong(arg))));
        parsers.put("immediate-profiling-duration", new ParseConfigValue<>(true, (config, arg) -> config.setImmediateProfilingDuration(Long.parseLong(arg))));
        parsers.put("settings-moniker", new ParseConfigValue<>(true, (config, arg) -> config.setSettingsMoniker(arg)));

        return parseConfig(new CollectionPlanConfigurationBuilder(), tokens, parsers).createDefaultConfiguration();
    }

    public static DefaultConfiguration parseDefaultConfiguration(String defaultConfig) {
        if (defaultConfig == null) {
            return new DefaultConfiguration(false, 0, 0);
        }

        String[] tokens = defaultConfig.split(" ");

        Map<String, ParseConfigValue<DefaultConfigurationBuilder>> parsers = new HashMap<>();
        parsers.put("sampling-profiling-duration", new ParseConfigValue<>(true, (config, arg) -> config.setSamplingProfileDuration(Long.parseLong(arg))));
        parsers.put("sampling-rate", new ParseConfigValue<>(true, (config, arg) -> config.setSamplingRate(Integer.parseInt(arg))));
        parsers.put("sampling-enabled", new ParseConfigValue<>(true, (config, arg) -> config.setSamplingEnabled(Boolean.parseBoolean(arg))));

        return parseConfig(new DefaultConfigurationBuilder(), tokens, parsers).createDefaultConfiguration();
    }

    public static AlertConfiguration parseFromMemory(String memoryConfig) {
        if (memoryConfig == null) {
            return new AlertConfiguration(AlertMetricType.MEMORY, false, 0f, 0, 0);
        }
        String[] tokens = memoryConfig.split(" ");

        Map<String, ParseConfigValue<AlertConfigurationBuilder>> parsers = new HashMap<>();
        parsers.put("memory-threshold", new ParseConfigValue<>(true, (config, arg) -> config.setThreshold(Float.parseFloat(arg))));
        parsers.put("memory-trigger-cooldown", new ParseConfigValue<>(true, (config, arg) -> config.setCooldown(Long.parseLong(arg))));
        parsers.put("memory-trigger-profilingDuration", new ParseConfigValue<>(true, (config, arg) -> config.setProfileDuration(Long.parseLong(arg))));
        parsers.put("memory-trigger-enabled", new ParseConfigValue<>(true, (config, arg) -> config.setEnabled(Boolean.parseBoolean(arg))));

        return parseConfig(new AlertConfigurationBuilder(), tokens, parsers)
                .setType(AlertMetricType.MEMORY)
                .createAlertConfiguration();
    }

    public static AlertConfiguration parseFromCpu(String cpuConfig) {
        if (cpuConfig == null) {
            return new AlertConfiguration(AlertMetricType.CPU, false, 0f, 0, 0);
        }

        String[] tokens = cpuConfig.split(" ");

        Map<String, ParseConfigValue<AlertConfigurationBuilder>> parsers = new HashMap<>();
        parsers.put("cpu-threshold", new ParseConfigValue<>(true, (config, arg) -> config.setThreshold(Float.parseFloat(arg))));
        parsers.put("cpu-trigger-cooldown", new ParseConfigValue<>(true, (config, arg) -> config.setCooldown(Long.parseLong(arg))));
        parsers.put("cpu-trigger-profilingDuration", new ParseConfigValue<>(true, (config, arg) -> config.setProfileDuration(Long.parseLong(arg))));
        parsers.put("cpu-trigger-enabled", new ParseConfigValue<>(true, (config, arg) -> config.setEnabled(Boolean.parseBoolean(arg))));

        return parseConfig(new AlertConfigurationBuilder(), tokens, parsers)
                .setType(AlertMetricType.CPU)
                .createAlertConfiguration();
    }


    private interface ConfigParser<T> {
        T parse(T config, String arg);
    }

    private static class ParseConfigValue<T> {
        private final boolean hasArg;
        private final ConfigParser<T> configParser;

        private ParseConfigValue(boolean hasArg, ConfigParser<T> configParser) {
            this.hasArg = hasArg;
            this.configParser = configParser;
        }
    }

    private static <T> T parseConfig(T builder, String[] tokens, Map<String, ParseConfigValue<T>> parsers) {
        for (int index = 0; index < tokens.length; index++) {
            if (tokens[index] != null && !tokens[index].isEmpty()) {
                ParseConfigValue<T> parser = parsers.get(tokens[index].replaceFirst("--", ""));

                if (parser != null) {
                    if (parser.hasArg && index + 1 < tokens.length) {
                        String arg = tokens[++index];
                        parser.configParser.parse(builder, arg);
                    } else if (!parser.hasArg) {
                        parser.configParser.parse(builder, null);
                    }
                }
            }
        }
        return builder;
    }

    public static AlertingConfiguration toAlertingConfig(ProfilerConfiguration profilerConfiguration) {
        return AlertConfigParser.parse(
                profilerConfiguration.getCpuTriggerConfiguration(),
                profilerConfiguration.getMemoryTriggerConfiguration(),
                profilerConfiguration.getDefaultConfiguration(),
                profilerConfiguration.getCollectionPlan()
        );
    }
}
