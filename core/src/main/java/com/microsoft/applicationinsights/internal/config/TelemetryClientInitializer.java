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

package com.microsoft.applicationinsights.internal.config;

import com.microsoft.applicationinsights.common.Strings;
import com.microsoft.applicationinsights.internal.heartbeat.HeartBeatModule;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.extensibility.*;
import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.internal.perfcounter.JmxMetricPerformanceCounter;
import com.microsoft.applicationinsights.internal.perfcounter.JvmPerformanceCountersModule;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterContainer;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterConfigurationAware;

import com.microsoft.applicationinsights.internal.perfcounter.ProcessPerformanceCountersModule;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer class for telemetry client instances.
 */
public enum TelemetryClientInitializer {
    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(TelemetryClientInitializer.class);

    private static final Set<String> defaultPerformaceModuleClassNames = new HashSet<>();

    static {
        addDefaultPerfModuleClassName(ProcessPerformanceCountersModule.class.getCanonicalName());
        addDefaultPerfModuleClassName(JvmPerformanceCountersModule.class.getCanonicalName());
    }

    public static synchronized void addDefaultPerfModuleClassName(String name) {
        defaultPerformaceModuleClassNames.add(name);
    }

    TelemetryClientInitializer() {
    }

    /**
     * Currently we do the following:
     *
     * Set Instrumentation Key
     * Set Developer Mode (default false)
     * Set Channel
     * Set Tracking Disabled Mode (default false)
     * Set Context Initializers where they should be written with full package name
     * Set Telemetry Initializers where they should be written with full package name
     * @param telemetryClient The configuration that will be populated
     */
    public void initialize(TelemetryClient telemetryClient,
                           ApplicationInsightsXmlConfiguration applicationInsightsConfig) {

        setConnectionString(applicationInsightsConfig, telemetryClient);
        setRoleName(applicationInsightsConfig, telemetryClient);
        setRoleInstance(applicationInsightsConfig, telemetryClient);

        setTelemetryModules(applicationInsightsConfig, telemetryClient);

        setQuickPulse(applicationInsightsConfig, telemetryClient);

        initializeComponents(telemetryClient);
    }

    private void setQuickPulse(ApplicationInsightsXmlConfiguration appConfiguration, TelemetryClient telemetryClient) {
        if (isQuickPulseEnabledInConfiguration(appConfiguration)) {
            logger.trace("Initializing QuickPulse...");
            QuickPulse.INSTANCE.initialize(telemetryClient);
        }
    }

    private boolean isQuickPulseEnabledInConfiguration(ApplicationInsightsXmlConfiguration appConfiguration) {
        QuickPulseXmlElement quickPulseXmlElement = appConfiguration.getQuickPulse();
        return quickPulseXmlElement.isEnabled();
    }

    /**
     * Sets the configuration data of Modules Initializers in configuration class.
     * @param appConfiguration The configuration data.
     * @param telemetryClient The configuration class.
     */
    private void setTelemetryModules(ApplicationInsightsXmlConfiguration appConfiguration, TelemetryClient telemetryClient) {
        TelemetryModulesXmlElement configurationModules = appConfiguration.getModules();
        List<TelemetryModule> modules = telemetryClient.getTelemetryModules();

        if (configurationModules != null) {
            ReflectionUtils.loadComponents(TelemetryModule.class, modules, configurationModules.getAdds());
        }

        //if heartbeat module is not loaded, load heartbeat module
        if (!isHeartBeatModuleAdded(modules)) {
            addHeartBeatModule(telemetryClient);
        }

        List<TelemetryModule> pcModules = getPerformanceModules(appConfiguration.getPerformance());

        modules.addAll(pcModules);
    }

    private void setConnectionString(ApplicationInsightsXmlConfiguration configXml, TelemetryClient telemetryClient) {

        String connectionString = configXml.getConnectionString();

        if (connectionString != null) {
            telemetryClient.setConnectionString(connectionString);
        }
    }

    private void setRoleName(ApplicationInsightsXmlConfiguration userConfiguration,
                             TelemetryClient telemetryClient) {
        try {
            String roleName;

            // try to find the role name in ApplicationInsights.xml
            if (userConfiguration != null) {
                roleName = userConfiguration.getRoleName();
                if (roleName == null) {
                    return;
                }

                roleName = roleName.trim();
                if (roleName.length() == 0) {
                    return;
                }

                telemetryClient.setRoleName(roleName);
            }
        } catch (Exception e) {
            logger.error("Failed to set role name: '{}'", e.toString());
        }
    }

    private void setRoleInstance(ApplicationInsightsXmlConfiguration userConfiguration,
                             TelemetryClient telemetryClient) {
        try {
            String roleInstance;

            // try to find the role instance in ApplicationInsights.xml
            if (userConfiguration != null) {
                roleInstance = userConfiguration.getRoleInstance();
                if (roleInstance == null) {
                    return;
                }

                roleInstance = roleInstance.trim();
                if (roleInstance.length() == 0) {
                    return;
                }

                telemetryClient.setRoleInstance(roleInstance);
            }
        } catch (Exception e) {
            logger.error("Failed to set role instance: '{}'", e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private List<TelemetryModule> getPerformanceModules(PerformanceCountersXmlElement performanceConfigurationData) {
        PerformanceCounterContainer.INSTANCE.setCollectionFrequencyInSec(performanceConfigurationData.getCollectionFrequencyInSec());

        ArrayList<TelemetryModule> modules = new ArrayList<>();

        List<String> performanceModuleNames = new ArrayList<>();
        if (performanceModuleNames.size() == 0) {
            // Only a workaround for JBoss web servers.
            // Will be removed once the issue will be investigated and fixed.
            logger.trace("Default performance counters will be automatically loaded.");
            performanceModuleNames.addAll(getDefaultPerformanceModulesNames());
        }

        for (String performanceModuleName : performanceModuleNames) {
            TelemetryModule module = ReflectionUtils.createInstance(performanceModuleName, TelemetryModule.class);
            if (module != null) {
                if (module instanceof PerformanceCounterConfigurationAware) {
                    PerformanceCounterConfigurationAware awareModule = (PerformanceCounterConfigurationAware)module;
                    try {
                        awareModule.addConfigurationData(performanceConfigurationData);
                    } catch (Exception e) {
                        logger.error("Failed to add configuration data to performance module: '{}'", e.toString());
                    }
                }
                modules.add(module);
            } else {
                logger.error("Failed to create performance module: '{}'", performanceModuleName);
            }
        }

        loadCustomJmxPCs(performanceConfigurationData.getJmxXmlElements());

        return modules;
    }

    /**
     * This method is only a workaround until the failure to load PCs in JBoss web servers will be solved.
     */
    private Set<String> getDefaultPerformanceModulesNames() {
        return defaultPerformaceModuleClassNames;
    }

    /**
     * The method will load the Jmx performance counters requested by the user to the system:
     * 1. Build a map where the key is the Jmx object name and the value is a list of requested attributes.
     * 2. Go through all the requested Jmx counters:
     *      a. If the object name is not in the map, add it with an empty list
     *         Else get the list
     *      b. Add the attribute to the list.
     *  3. Go through the map
     *      For every entry (object name and attributes)
     *          Build a {@link JmxMetricPerformanceCounter}
     *          Register the Performance Counter in the {@link PerformanceCounterContainer}
     *
     * @param jmxXmlElements
     */
    private void loadCustomJmxPCs(ArrayList<JmxXmlElement> jmxXmlElements) {
        try {
            if (jmxXmlElements == null) {
                return;
            }

            HashMap<String, Collection<JmxAttributeData>> data = new HashMap<>();

            // Build a map of object name to its requested attributes
            for (JmxXmlElement jmxElement : jmxXmlElements) {
                Collection<JmxAttributeData> collection = data.get(jmxElement.getObjectName());
                if (collection == null) {
                    collection = new ArrayList<>();
                    data.put(jmxElement.getObjectName(), collection);
                }

                if (Strings.isNullOrEmpty(jmxElement.getObjectName())) {
                    logger.error("JMX object name is empty, will be ignored");
                    continue;
                }

                if (Strings.isNullOrEmpty(jmxElement.getAttribute())) {
                    logger.error("JMX attribute is empty for '{}', will be ignored", jmxElement.getObjectName());
                    continue;
                }

                if (Strings.isNullOrEmpty(jmxElement.getName())) {
                    logger.error("JMX name is empty for '{}', will be ignored", jmxElement.getObjectName());
                    continue;
                }

                collection.add(new JmxAttributeData(jmxElement.getName(), jmxElement.getAttribute()));
            }

            // Register each entry in the performance container
            for (Map.Entry<String, Collection<JmxAttributeData>> entry : data.entrySet()) {
                try {
                    if (PerformanceCounterContainer.INSTANCE.register(new JmxMetricPerformanceCounter(entry.getKey(), entry.getKey(), entry.getValue()))) {
                        logger.trace("Registered JMX performance counter '{}'", entry.getKey());
                    } else {
                        logger.trace("Failed to register JMX performance counter '{}'", entry.getKey());
                    }
                } catch (Exception e) {
                    logger.error("Failed to register JMX performance counter '{}': '{}'", entry.getKey(), e.toString());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to register JMX performance counters: '{}'", e.toString());
        }
    }

    private void initializeComponents(TelemetryClient telemetryClient) {
        List<TelemetryModule> telemetryModules = telemetryClient.getTelemetryModules();

        for (TelemetryModule module : telemetryModules) {
            try {
                module.initialize(telemetryClient);
            } catch (Exception e) {
                logger.error(
                        "Failed to initialized telemetry module " + module.getClass().getSimpleName() + ". Exception");
            }
        }
    }

    /**
     * Adds heartbeat module with default configuration
     * @param telemetryClient telemetry client instance
     */
    private void addHeartBeatModule(TelemetryClient telemetryClient) {
        HeartBeatModule module = new HeartBeatModule(new HashMap<>());
        telemetryClient.getTelemetryModules().add(module);
    }

    /**
     * Checks if heartbeat module is present
     * @param module List of modules in current TelemetryClient instance
     * @return true if heartbeat module is present
     */
    private boolean isHeartBeatModuleAdded(List<TelemetryModule> module) {
        for (TelemetryModule mod : module) {
            if (mod instanceof HeartBeatModule) return true;
        }
        return false;
    }
}
