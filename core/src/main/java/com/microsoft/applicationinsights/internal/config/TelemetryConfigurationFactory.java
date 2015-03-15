/*
 * AppInsights-Java
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.annotation.PerformanceModule;
import eu.infomas.annotation.AnnotationDetector;

import static eu.infomas.annotation.AnnotationDetector.*;

/**
 * Initializer class for configuration instances.
 */
public enum TelemetryConfigurationFactory {
    INSTANCE;

    // Default file name
    private final static String CONFIG_FILE_NAME = "ApplicationInsights.xml";

    private final static String CONTEXT_INITIALIZERS_SECTION = "ContextInitializers";
    private final static String TELEMETRY_INITIALIZERS_SECTION = "TelemetryInitializers";
    private final static String TELEMETRY_MODULES_SECTION = "TelemetryModules";
    private final static String INITIALIZERS_ADD = "Add";
    private final static String CLASS_TYPE_AS_ATTRIBUTE = "type";
    private final static String CHANNEL_SECTION = "Channel";
    private final static String DISABLE_TELEMETRY_SECTION = "DisableTelemetry";
    private final static String INSTRUMENTATION_KEY_SECTION = "InstrumentationKey";
    private final static String LOGGER_SECTION = "SDKLogger";
    private final static String PERFORMANCE_COUNTERS_SECTION = "PerformanceCounters";
    private final static String PERFORMANCE_BUILT_IN_COUNTERS = "BuiltIn";

    private final static String PERFORMANCE_MODULE_INTERNAL_FOLDERS = "com.microsoft.applicationinsights";

    private ConfigFileParser parser;
    private String fileToParse;
    private String performanceCountersSection = PERFORMANCE_MODULE_INTERNAL_FOLDERS;

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
            if (parser == null) {
                parser = new XmlConfigParser();
            }

            if (!parser.parse(fileToParse)) {
                configuration.setChannel(new InProcessTelemetryChannel());
                return;
            }

            // Set the logger first so we might have possible errors written
            setInternalLogger(parser, configuration);

            setInstrumentationKey(parser, configuration);

            if (!setChannel(parser, configuration)) {
                return;
            }

            setTrackingDisabledMode(parser, configuration);

            setContextInitializers(parser, configuration);
            setTelemetryInitializers(parser, configuration);
            setTelemetryModules(parser, configuration);

            initializeComponents(configuration);
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to initialize configuration, exception: %s", e.getMessage());
        }
    }

    public void setParserData(ConfigFileParser parser, String fileToParse) {
        this.fileToParse = fileToParse;
        this.parser = parser;
    }

    void setPerformanceCountersSection(String value) {
        performanceCountersSection = value;
    }

    private void setTrackingDisabledMode(ConfigFileParser parser, TelemetryConfiguration configuration) {
        configuration.setTrackingIsDisabled(fetchBooleanValue(parser, DISABLE_TELEMETRY_SECTION, false));
    }

    private void setInternalLogger(ConfigFileParser parser, TelemetryConfiguration configuration) {
        ConfigFileParser.StructuredDataResult loggerData = parser.getStructuredData(LOGGER_SECTION, CLASS_TYPE_AS_ATTRIBUTE);
        if (!loggerData.found) {
            return;
        }

        // The logger output type
        String loggerOutput = loggerData.sectionTag;

        InternalLogger.INSTANCE.initialize(loggerOutput, loggerData.items);
    }

    private List<TelemetryModule> getPerformanceModules(ConfigFileParser parser) {
        ArrayList<TelemetryModule> modules = new ArrayList<TelemetryModule>();

        ConfigFileParser.StructuredDataResult pcData = parser.getStructuredData(PERFORMANCE_COUNTERS_SECTION, null);
        if (!pcData.found) {
            return modules;
        }

        if (!pcData.items.containsKey(PERFORMANCE_BUILT_IN_COUNTERS)) {
            pcData.items.put(PERFORMANCE_BUILT_IN_COUNTERS, "");
        }

        final List<String> performanceModuleNames = new AnnotationPackageScanner().scanForClassAnnotations(new Class[]{PerformanceModule.class}, performanceCountersSection);
        for (String performanceModuleName : performanceModuleNames) {
            TelemetryModule module = createInstance(performanceModuleName, TelemetryModule.class);
            PerformanceModule pmAnnotation = module.getClass().getAnnotation(PerformanceModule.class);
            if (!pcData.items.containsKey(pmAnnotation.value())) {
                continue;
            }
            if (module != null) {
                modules.add(module);
            } else {
                InternalLogger.INSTANCE.error("Failed to create performance module: '%s'", performanceModuleName);
            }
        }

        return modules;
    }

    /**
     * Currently we only search for the name of the instance to create
     *
     * If not found or corrupted, we use {@link InProcessTelemetryChannel}
     *
     * @param parser The parser we work with
     * @param configuration Where we store the {@link com.microsoft.applicationinsights.channel.TelemetryChannel}
     * @return True on success
     */
    private boolean setChannel(ConfigFileParser parser, TelemetryConfiguration configuration) {
        ConfigFileParser.StructuredDataResult channelData = parser.getStructuredData(CHANNEL_SECTION, CLASS_TYPE_AS_ATTRIBUTE);

        if (channelData.found) {
            String channelName = channelData.sectionTag;

            if (channelName != null) {
                TelemetryChannel channel = createInstance(channelName, TelemetryChannel.class, Map.class, channelData.items);
                if (channel != null) {
                    configuration.setChannel(channel);
                    return true;
                }
            }
        }

        try {
            // We will create the default channel and we assume that the data is relevant.
            configuration.setChannel(new InProcessTelemetryChannel(channelData.items));
            return true;
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to create InProcessTelemetryChannel, exception: %s, will create the default one with default arguments", e.getMessage());
            configuration.setChannel(new InProcessTelemetryChannel());
            return true;
        }
    }

    /**
     * Setting an instrumentation key
     * @param parser The parser we work with
     * @param configuration Where we store our findings
     * @return True if success, false otherwise
     */
    private boolean setInstrumentationKey(ConfigFileParser parser, TelemetryConfiguration configuration) {
        try {
            String iKey = parser.getTrimmedValue(INSTRUMENTATION_KEY_SECTION);

            configuration.setInstrumentationKey(iKey);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Searches for the CONTEXT_INITIALIZERS_SECTION amd will fetch and create all instances
     * that are mentioned there. Those instances will be later be stored in the {@link com.microsoft.applicationinsights.TelemetryConfiguration}
     *
     * Currently, we 'hard code' putting three Initializers
     *
     * @param parser The parser we work to fetch the data
     * @param configuration Where we need to store our new instances
     */
    private void setContextInitializers(ConfigFileParser parser, TelemetryConfiguration configuration) {
        List<ContextInitializer> initializerList = configuration.getContextInitializers();

        // To keep with prev version. A few will probably be moved to the configuration
        initializerList.add(new SdkVersionContextInitializer());
        initializerList.add(new DeviceInfoContextInitializer());

        loadComponents(ContextInitializer.class, parser, CONTEXT_INITIALIZERS_SECTION, INITIALIZERS_ADD, initializerList);
    }

    /**
     * Searches for the TELEMETRY_INITIALIZERS_SECTION amd will fetch and create all instances
     * that are mentioned there. Those instances will be later be stored in the {@link com.microsoft.applicationinsights.TelemetryConfiguration}
     * @param parser The parser we work to fetch the data
     * @param configuration Where we need to store our new instances
     */
    private void setTelemetryInitializers(ConfigFileParser parser, TelemetryConfiguration configuration) {
        List<TelemetryInitializer> initializerList = configuration.getTelemetryInitializers();
        loadComponents(TelemetryInitializer.class, parser, TELEMETRY_INITIALIZERS_SECTION, INITIALIZERS_ADD, initializerList);
    }

    private void setTelemetryModules(ConfigFileParser parser, TelemetryConfiguration configuration) {
        List<TelemetryModule> modules = configuration.getTelemetryModules();
        loadComponents(TelemetryModule.class, parser, TELEMETRY_MODULES_SECTION, INITIALIZERS_ADD, modules);

        List<TelemetryModule> pcModules = getPerformanceModules(parser);
        modules.addAll(pcModules);
    }

    /**
     * An helper method that fetches a boolean value of configuration file.
     * If not found, or corrupted, a default value will be returned
     * @param parser The parser we work with to fetch the value
     * @param tagName The name of the tag where the parser should look for the value
     * @param defaultValue A value to be returned in case the parser fails to find or the value is corrupted
     * @return The value or default
     */
    private boolean fetchBooleanValue(ConfigFileParser parser, String tagName, boolean defaultValue) {
        boolean result = defaultValue;

        String value = parser.getTrimmedValue(tagName);
        if (!Strings.isNullOrEmpty(value)) {
            result = Boolean.valueOf(value);
        }

        return result;
    }

    /**
     * Generic method that creates instances based on their names and adds them to a Collection
     *
     * Note that the class does its 'best effort' to create an instance and will not fail the method
     * if an instance (or more) was failed to create. This is naturally, a policy we can easily replace
     *
     * @param clazz The class all instances should have
     * @param parser The parser gives us the names of the classes to create
     * @param sectionName The section name where we tell the parser to search
     * @param itemName The internal name inside the section name, to point the parser
     * @param list The container of instances, this is where we store our instances that we create
     * @param <T>
     */
    private <T> void loadComponents(
            Class<T> clazz,
            ConfigFileParser parser,
            String sectionName,
            String itemName,
            List<T> list) {
        Collection<String> classNames = parser.getList(sectionName, itemName, CLASS_TYPE_AS_ATTRIBUTE);
        for (String className : classNames) {
            T initializer = createInstance(className, clazz);
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
}
