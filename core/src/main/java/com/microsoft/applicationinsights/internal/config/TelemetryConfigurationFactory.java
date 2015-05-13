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

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.initializer.DeviceInfoContextInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.SdkVersionContextInitializer;
import com.microsoft.applicationinsights.internal.annotation.AnnotationPackageScanner;
import com.microsoft.applicationinsights.internal.annotation.PerformanceModule;
import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.perfcounter.JmxMetricPerformanceCounter;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterContainer;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterConfigurationAware;

/**
 * Initializer class for configuration instances.
 */
public enum TelemetryConfigurationFactory {
    INSTANCE;

    // Default file name
    private final static String CONFIG_FILE_NAME = "ApplicationInsights.xml";
    private final static String DEFAULT_PERFORMANCE_MODULES_PACKAGE = "com.microsoft.applicationinsights";
    private final static String BUILT_IN_NAME = "BuiltIn";

    private String fileToParse;
    private String performanceCountersSection = DEFAULT_PERFORMANCE_MODULES_PACKAGE;

    private AppInsightsConfigurationBuilder builder = new JaxbAppInsightsConfigurationBuilder();

    TelemetryConfigurationFactory() {
        fileToParse = CONFIG_FILE_NAME;
    }

    /**
     * Currently we do the following:
     *
     * Set Instrumentation Key
     * Set Developer Mode (default false)
     * Set Channel (default {@link InProcessTelemetryChannel})
     * Set Tracking Disabled Mode (default false)
     * Set Context Initializers where they should be written with full package name
     * Set Telemetry Initializers where they should be written with full package name
     * @param configuration The configuration that will be populated
     */
    public final void initialize(TelemetryConfiguration configuration) {
        try {
            InternalLogger.INSTANCE.configMode(true);

            String configurationFileName = getConfigurationFileName();
            if (Strings.isNullOrEmpty(configurationFileName)) {
                return;
            }

            ApplicationInsightsXmlConfiguration applicationInsights = builder.build(configurationFileName);
            if (applicationInsights == null) {
                InternalLogger.INSTANCE.logConfig(InternalLogger.LoggingLevel.ERROR, "Failed to read configuration file");
                configuration.setChannel(new InProcessTelemetryChannel());
                return;
            }

            setInternalLogger(applicationInsights.getSdkLogger(), configuration);

            setInstrumentationKey(applicationInsights.getInstrumentationKey(), configuration);

            setChannel(applicationInsights.getChannel(), configuration);

            configuration.setTrackingIsDisabled(applicationInsights.isDisableTelemetry());

            setContextInitializers(applicationInsights.getContextInitializers(), configuration);
            setTelemetryInitializers(applicationInsights.getTelemetryInitializers(), configuration);
            setTelemetryModules(applicationInsights, configuration);

            initializeComponents(configuration);
        } catch (Exception e) {
            InternalLogger.INSTANCE.logConfig(InternalLogger.LoggingLevel.ERROR, "Failed to initialize configuration, exception: %s", e.getMessage());
        } finally {
            InternalLogger.INSTANCE.configMode(false);
        }
    }

    private void setInternalLogger(SDKLoggerXmlElement sdkLogger, TelemetryConfiguration configuration) {
        if (sdkLogger == null) {
            return;
        }

        InternalLogger.INSTANCE.initialize(sdkLogger.getType(), sdkLogger.getData());
    }

    /**
     * Sets the configuration data of Telemetry Initializers in configuration class.
     * @param telemetryInitializers The configuration data.
     * @param configuration The configuration class.
     */
    private void setTelemetryInitializers(TelemetryInitializersXmlElement telemetryInitializers, TelemetryConfiguration configuration) {
        if (telemetryInitializers == null) {
            return;
        }

        List<TelemetryInitializer> initializerList = configuration.getTelemetryInitializers();
        loadComponents(TelemetryInitializer.class, initializerList, telemetryInitializers.getAdds());
    }

    /**
     * Sets the configuration data of Context Initializers in configuration class.
     * @param contextInitializers The configuration data.
     * @param configuration The configuration class.
     */
    private void setContextInitializers(ContextInitializersXmlElement contextInitializers, TelemetryConfiguration configuration) {
        List<ContextInitializer> initializerList = configuration.getContextInitializers();

        // To keep with prev version. A few will probably be moved to the configuration
        initializerList.add(new SdkVersionContextInitializer());
        initializerList.add(new DeviceInfoContextInitializer());

        if (contextInitializers != null) {
            loadComponents(ContextInitializer.class, initializerList, contextInitializers.getAdds());
        }
    }

    /**
     * Sets the configuration data of Modules Initializers in configuration class.
     * @param appConfiguration The configuration data.
     * @param configuration The configuration class.
     */
    private void setTelemetryModules(ApplicationInsightsXmlConfiguration appConfiguration, TelemetryConfiguration configuration) {
        TelemetryModulesXmlElement configurationModules = appConfiguration.getModules();
        List<TelemetryModule> modules = configuration.getTelemetryModules();

        if (configurationModules != null) {
            loadComponents(TelemetryModule.class, modules, configurationModules.getAdds());
        }

        List<TelemetryModule> pcModules = getPerformanceModules(appConfiguration.getPerformance());
        modules.addAll(pcModules);
    }

    /**
     * Setting an instrumentation key
     * @param instrumentationKey The instrumentation key found in the configuration.
     * @param configuration The configuration class.
     * @return True if succeeded.
     */
    private boolean setInstrumentationKey(String instrumentationKey, TelemetryConfiguration configuration) {
        try {
            configuration.setInstrumentationKey(instrumentationKey);

            return true;
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to set instrumentation key: '%s'", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<TelemetryModule> getPerformanceModules(PerformanceCountersXmlElement performanceConfigurationData) {
        ArrayList<TelemetryModule> modules = new ArrayList<TelemetryModule>();

        final List<String> performanceModuleNames =
                new AnnotationPackageScanner().scanForClassAnnotations(new Class[]{PerformanceModule.class}, performanceCountersSection);

        if (performanceModuleNames.size() == 0) {

            // Only a workaround for JBoss web servers.
            // Will be removed once the issue will be investigated and fixed.
            performanceModuleNames.addAll(getDefaultPerformanceModulesNames());
        }

        for (String performanceModuleName : performanceModuleNames) {
            TelemetryModule module = createInstance(performanceModuleName, TelemetryModule.class);
            if (module != null) {
                PerformanceModule pmAnnotation = module.getClass().getAnnotation(PerformanceModule.class);
                if (!performanceConfigurationData.isUseBuiltIn() && BUILT_IN_NAME.equals(pmAnnotation.value())) {
                    continue;
                }
                if (module instanceof PerformanceCounterConfigurationAware) {
                    PerformanceCounterConfigurationAware awareModule = (PerformanceCounterConfigurationAware)module;
                    try {
                        awareModule.addConfigurationData(performanceConfigurationData);
                    } catch (Exception e) {
                        InternalLogger.INSTANCE.error("Failed to add configuration data to performance module: '%s'", e.getMessage());
                    }
                }
                modules.add(module);
            } else {
                InternalLogger.INSTANCE.error("Failed to create performance module: '%s'", performanceModuleName);
            }
        }

        loadCustomJmxPCs(performanceConfigurationData.getJmxXmlElements());

        return modules;
    }

    /**
     * This method is only a workaround until the failure to load PCs in JBoss web servers will be solved.
     */
    private List<String> getDefaultPerformanceModulesNames() {
        InternalLogger.INSTANCE.trace("Default performance counters will be automatically loaded.");

        ArrayList<String> modules = new ArrayList<String>();
        modules.add("com.microsoft.applicationinsights.internal.perfcounter.ProcessPerformanceCountersModule");
        modules.add("com.microsoft.applicationinsights.web.internal.perfcounter.WebPerformanceCounterModule");

        return modules;
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

            HashMap<String, Collection<JmxAttributeData>> data = new HashMap<String, Collection<JmxAttributeData>>();

            // Build a map of object name to its requested attributes
            for (JmxXmlElement jmxElement : jmxXmlElements) {
                Collection<JmxAttributeData> collection = data.get(jmxElement.getObjectName());
                if (collection == null) {
                    collection = new ArrayList<JmxAttributeData>();
                    data.put(jmxElement.getObjectName(), collection);
                }

                if (Strings.isNullOrEmpty(jmxElement.getObjectName())) {
                    InternalLogger.INSTANCE.error("JMX object name is empty, will be ignored");
                    continue;
                }

                if (Strings.isNullOrEmpty(jmxElement.getAttribute())) {
                    InternalLogger.INSTANCE.error("JMX attribute is empty for '%s', will be ignored", jmxElement.getObjectName());
                    continue;
                }

                if (Strings.isNullOrEmpty(jmxElement.getDisplayName())) {
                    InternalLogger.INSTANCE.error("JMX display name is empty for '%s', will be ignored", jmxElement.getObjectName());
                    continue;
                }

                collection.add(new JmxAttributeData(jmxElement.getDisplayName(), jmxElement.getAttribute(), jmxElement.getType()));
            }

            // Register each entry in the performance container
            for (Map.Entry<String, Collection<JmxAttributeData>> entry : data.entrySet()) {
                try {
                    if (PerformanceCounterContainer.INSTANCE.register(new JmxMetricPerformanceCounter(entry.getKey(), entry.getKey(), entry.getValue()))) {
                        InternalLogger.INSTANCE.trace("Registered JMX performance counter '%s'", entry.getKey());
                    } else {
                        InternalLogger.INSTANCE.trace("Failed to register JMX performance counter '%s'", entry.getKey());
                    }
                } catch (Exception e) {
                    InternalLogger.INSTANCE.error("Failed to register JMX performance counter '%s': '%s'", entry.getKey(), e.getMessage());
                }
            }
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to register JMX performance counters: '%s'", e.getMessage());
        }
    }

    /**
     * Setting the channel.
     * @param channelXmlElement The configuration element holding the channel data.
     * @param configuration The configuration class.
     * @return True on success.
     */
    private boolean setChannel(ChannelXmlElement channelXmlElement, TelemetryConfiguration configuration) {
        String channelName = channelXmlElement.getType();
        if (channelName != null) {
            TelemetryChannel channel = createInstance(channelName, TelemetryChannel.class, Map.class, channelXmlElement.getData());
            if (channel != null) {
                configuration.setChannel(channel);
                return true;
            } else {
                InternalLogger.INSTANCE.error("Failed to create '%s', will create the default one with default arguments", channelName);
            }
        }

        try {
            // We will create the default channel and we assume that the data is relevant.
            configuration.setChannel(new InProcessTelemetryChannel(channelXmlElement.getData()));
            return true;
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to create InProcessTelemetryChannel, exception: %s, will create the default one with default arguments", e.getMessage());
            configuration.setChannel(new InProcessTelemetryChannel());
            return true;
        }
    }

    private String getConfigurationFileName() {

        // Trying to load configuration as a resource.
        ClassLoader classLoader = TelemetryConfigurationFactory.class.getClassLoader();
        URL resource = classLoader.getResource(fileToParse);

        if (resource == null) {
            InternalLogger.INSTANCE.logConfig(InternalLogger.LoggingLevel.INFO, "Did not find resource configuration file");

            // If not found as a resource, trying to load from the executing jar directory
            String configurationFile = getConfFromSourceLocation();
            if (configurationFile != null) {
                return configurationFile;
            }

            // If not found as a resource, trying to load from the class path
            return getConfFromClassPath(classLoader);
        } else {
            String configurationFile = resource.getFile();
            InternalLogger.INSTANCE.logConfig(InternalLogger.LoggingLevel.INFO, "Found configuration file: '%s'", configurationFile);
            return configurationFile;
        }
    }

    private String getConfFromClassPath(ClassLoader classLoader) {
        if (!(classLoader instanceof URLClassLoader)) {
            return null;
        }

        HashSet<String> checkedUrls = new HashSet<String>();

        URL[] urls = ((URLClassLoader)classLoader).getURLs();
        for (URL url : urls) {
            String configurationPath = url.getPath();
            if (configurationPath.endsWith(".jar")) {
                int index = configurationPath.lastIndexOf('/');
                if (index != -1) {
                    configurationPath = configurationPath.substring(0, index + 1);
                } else {
                    continue;
                }
            } else {
                if (!configurationPath.endsWith("/")) {
                    configurationPath += '/';
                }
            }

            if (checkedUrls.contains(configurationPath)) {
                continue;
            }

            String configurationFile = configurationFileExists(configurationPath);
            if (configurationFile != null) {
                return configurationFile;
            } else {
                checkedUrls.add(configurationPath);
            }
        }

        return null;
    }

    private String getConfFromSourceLocation() {
        try {
            String jarFullPath = TelemetryConfigurationFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarFullPath);

            if (jarFile.exists()) {
                String jarDirectory = jarFile.getParent();

                String configurationFile = configurationFileExists(jarDirectory);

                if (configurationFile != null) {
                    return jarDirectory;
                }
            } else {
                InternalLogger.INSTANCE.logConfig(InternalLogger.LoggingLevel.ERROR, "Can not access folder '%s'", jarFullPath);
            }
        } catch (URISyntaxException e) {
            InternalLogger.INSTANCE.logConfig(InternalLogger.LoggingLevel.ERROR, "Failed to find configuration file, exception: '%s'", e.getMessage());
        }

        return null;
    }

    private String configurationFileExists(String path) {
        File configFile = new File(path, fileToParse);

        if (configFile.exists()) {
            InternalLogger.INSTANCE.logConfig(InternalLogger.LoggingLevel.INFO, "Found configuration file in: '%s'", path);
            return configFile.getAbsolutePath();
        } else {
            InternalLogger.INSTANCE.logConfig(InternalLogger.LoggingLevel.INFO, "Did not find configuration file in '%s'", path);
        }

        return null;
    }

    /**
     * Generic method that creates instances based on their names and adds them to a Collection
     *
     * Note that the class does its 'best effort' to create an instance and will not fail the method
     * if an instance (or more) was failed to create. This is naturally, a policy we can easily replace
     *
     * @param clazz The class all instances should have
     * @param list The container of instances, this is where we store our instances that we create
     * @param classNames Classes to create.
     * @param <T>
     */
    private <T> void loadComponents(
            Class<T> clazz,
            List<T> list,
            Collection<AddTypeXmlElement> classNames) {
        if (classNames == null) {
            return;
        }

        for (AddTypeXmlElement className : classNames) {
            T initializer = null;

            // If parameters have been provided, we try to load the component with provided parameters map. Otherwise,
            // we fallback to initialize the component with the default ctor.
            if (className.getParameters().size() != 0) {
                initializer = createInstance(className.getType(), clazz, Map.class, className.getData());
            }

            if (initializer == null) {
                initializer = createInstance(className.getType(), clazz);
            }

            if (initializer != null) {
                list.add(initializer);
            }
        }
    }

    /**
     * Creates an instance from its name. We suppress Java compiler warnings for Generic casting
     *
     * Note that currently we 'swallow' all exceptions and simply return null if we fail
     *
     * @param className The class we create an instance of
     * @param interfaceClass The class' parent interface we wish to work with
     * @param <T> The class type to create
     * @return The instance or null if failed
     */
    @SuppressWarnings("unchecked")
    private <T> T createInstance(String className, Class<T> interfaceClass) {
        try {
            if (Strings.isNullOrEmpty(className)) {
                return null;
            }

            Class<?> clazz = Class.forName(className).asSubclass(interfaceClass);
            T instance = (T)clazz.newInstance();

            return instance;
        } catch (ClassCastException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        } catch (ClassNotFoundException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        } catch (InstantiationException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        } catch (IllegalAccessException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        }

        return null;
    }

    /**
     * Creates an instance from its name. We suppress Java compiler warnings for Generic casting
     * The class is created by using a constructor that has one parameter which is sent to the method
     *
     * Note that currently we 'swallow' all exceptions and simply return null if we fail
     *
     * @param className The class we create an instance of
     * @param interfaceClass The class' parent interface we wish to work with
     * @param argumentClass Type of class to use as argument for Ctor
     * @param argument The argument to pass the Ctor
     * @param <T> The class type to create
     * @param <A> The class type as the Ctor argument
     * @return The instance or null if failed
     */
    @SuppressWarnings("unchecked")
    private <T, A> T createInstance(String className, Class<T> interfaceClass, Class<A> argumentClass, A argument) {
        try {
            if (Strings.isNullOrEmpty(className)) {
                return null;
            }

            Class<?> clazz = Class.forName(className).asSubclass(interfaceClass);
            Constructor<?> clazzConstructor = clazz.getConstructor(argumentClass);
            T instance = (T)clazzConstructor.newInstance(argument);
            return instance;
        } catch (ClassCastException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        } catch (ClassNotFoundException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        } catch (InstantiationException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        } catch (IllegalAccessException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to create %s, %s", className, e.getMessage());
        }

        return null;
    }

    // TODO: include context/telemetry initializers - where do they initialized?
    private void initializeComponents(TelemetryConfiguration configuration) {
        List<TelemetryModule> telemetryModules = configuration.getTelemetryModules();

        for (TelemetryModule module : telemetryModules) {
            try {
                module.initialize(configuration);
            } catch (Exception e) {
                InternalLogger.INSTANCE.error(
                        "Failed to initialized telemetry module " + module.getClass().getSimpleName() + ". Excepption");
            }
        }
    }

    void setPerformanceCountersSection(String performanceCountersSection) {
        this.performanceCountersSection = performanceCountersSection;
    }

    void setBuilder(AppInsightsConfigurationBuilder builder) {
        this.builder = builder;
    }
}
