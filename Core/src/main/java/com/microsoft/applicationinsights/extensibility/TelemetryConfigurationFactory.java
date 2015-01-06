package com.microsoft.applicationinsights.extensibility;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.HashSet;
import java.util.List;

import com.microsoft.applicationinsights.channel.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.implementation.DeviceInfoContextInitializer;
import com.microsoft.applicationinsights.implementation.PropertiesContextInitializer;
import com.microsoft.applicationinsights.implementation.SdkVersionContextInitializer;

import com.google.common.base.Strings;

/**
 * Initializer class for configuration instances.
 */
enum TelemetryConfigurationFactory {
    INSTANCE;

    // Default file name
    private final static String CONFIG_FILE_NAME = "ApplicationInsights.xml";

    private final static String CONTEXT_INITIALIZERS_SECTION = "ContextInitializers";
    private final static String TELEMETRY_INITIALIZERS_SECTION = "TelemetryInitializers";
    private final static String INITIALIZERS_ADD = "Add";
    private final static String CLASS_TYPE = "Type";
    private final static String CHANNEL_SECTION = "Channel";
    private final static String DISABLE_TELEMETRY_SECTION = "DisableTelemetry";
    private final static String DEVELOPER_MODE_SECTION = "DeveloperMode";
    private final static String INSTRUMENTATION_KEY_SECTION = "InstrumentationKey";
    private final static String CHANNEL_ENDPONT_ADDRESS = "EndpointAddress";

    private ConfigFileParser parser;
    private String fileToParse;

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
     * @param configuration
     */
    public final void initialize(TelemetryClientConfiguration configuration) {
        try {
            if (parser == null) {
                parser = new XmlConfigParser();
            }

            if (!parser.parse(fileToParse)) {
                configuration.setDeveloperMode(false);
                configuration.setChannel(new InProcessTelemetryChannel(configuration));
                return;
            }

            setInstrumentationKey(parser, configuration);

            setDeveloperMode(parser, configuration);

            if (!setChannel(parser, configuration)) {
                return;
            }

            setTrackingDisabledMode(parser, configuration);

            setContextInitializers(parser, configuration);
            setTelemetryInitializers(parser, configuration);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public void setParserData(ConfigFileParser parser, String fileToParse) {
        this.fileToParse = fileToParse;
        this.parser = parser;
    }

    private void setTrackingDisabledMode(ConfigFileParser parser, TelemetryClientConfiguration configuration) {
        configuration.setTrackingIsDisabled(fetchBooleanValue(parser, DISABLE_TELEMETRY_SECTION, false));
    }

    private void setDeveloperMode(ConfigFileParser parser, TelemetryClientConfiguration configuration) {
        configuration.setDeveloperMode(fetchBooleanValue(parser, DEVELOPER_MODE_SECTION, false));
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
    private boolean setChannel(ConfigFileParser parser, TelemetryClientConfiguration configuration) {
        HashSet<String> itemNames = new HashSet<String>();
        itemNames.add(CLASS_TYPE);
        itemNames.add(CHANNEL_ENDPONT_ADDRESS);
        Map<String, String> channelData = parser.getStructuredData(CHANNEL_SECTION, itemNames);

        String channelEndpoint = channelData.get(CHANNEL_ENDPONT_ADDRESS);
        configuration.setEndpoint(channelEndpoint);

        String channelName = channelData.get(CLASS_TYPE);

        if (channelName != null) {
            TelemetryChannel channel = createInstance(channelName, TelemetryChannel.class, TelemetryClientConfiguration.class, configuration);
            if (channel != null) {
                configuration.setChannel(channel);
                return true;
            }
        }

        try {
            configuration.setChannel(new InProcessTelemetryChannel(configuration));
            return true;
        } catch (Exception e) {
            // e.printStackTrace();
        }

        return false;
    }

    /**
     * Setting an instrumentation key
     * @param parser The parser we work with
     * @param configuration Where we store our findings
     * @return True if success, false otherwise
     */
    private boolean setInstrumentationKey(ConfigFileParser parser, TelemetryClientConfiguration configuration) {
        String iKey = parser.getTrimmedValue(INSTRUMENTATION_KEY_SECTION);

        if (Strings.isNullOrEmpty(iKey)) {
            return false;
        }

        configuration.setInstrumentationKey(iKey);

        return true;
    }

    /**
     * Searches for the CONTEXT_INITIALIZERS_SECTION amd will fetch and create all instances
     * that are mentioned there. Those instances will be later be stored in the {@link com.microsoft.applicationinsights.extensibility.TelemetryConfiguration}
     *
     * Currently, we 'hard code' putting three Initializers
     *
     * @param parser The parser we work to fetch the data
     * @param configuration Where we need to store our new instances
     */
    private void setContextInitializers(ConfigFileParser parser, TelemetryClientConfiguration configuration) {
        List<ContextInitializer> initializerList = configuration.getContextInitializers();

        // To keep with prev version. A few will probably be moved to the configuration
        initializerList.add(new SdkVersionContextInitializer());
        initializerList.add(new DeviceInfoContextInitializer());
        initializerList.add(new PropertiesContextInitializer());

        setInitializers(ContextInitializer.class, parser, CONTEXT_INITIALIZERS_SECTION, INITIALIZERS_ADD, initializerList);
    }

    /**
     * Searches for the TELEMETRY_INITIALIZERS_SECTION amd will fetch and create all instances
     * that are mentioned there. Those instances will be later be stored in the {@link com.microsoft.applicationinsights.extensibility.TelemetryConfiguration}
     * @param parser The parser we work to fetch the data
     * @param configuration Where we need to store our new instances
     */
    private void setTelemetryInitializers(ConfigFileParser parser, TelemetryClientConfiguration configuration) {
        List<TelemetryInitializer> initializerList = configuration.getTelemetryInitializers();
        setInitializers(TelemetryInitializer.class, parser, TELEMETRY_INITIALIZERS_SECTION, INITIALIZERS_ADD, initializerList);
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
    private <T> void setInitializers(
            Class<T> clazz,
            ConfigFileParser parser,
            String sectionName,
            String itemName,
            List<T> list) {
        Collection<String> classNames = parser.getList(sectionName, itemName, CLASS_TYPE);
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
            Class<?> clazz = Class.forName(className).asSubclass(interfaceClass);
            T instance = (T)clazz.newInstance();
            return instance;
        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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
     * @param argumentClass Type of class to use as argument for ctor
     * @param argument The argument to pass the ctor
     * @param <T> The class type to create
     * @param <A> The class type as the ctor argument
     * @return The instance or null if failed
     */
    @SuppressWarnings("unchecked")
    private <T, A> T createInstance(String className, Class<T> interfaceClass, Class<A> argumentClass, A argument) {
        try {
            Class<?> clazz = Class.forName(className).asSubclass(interfaceClass);
            Constructor<?> clazzConstructor = clazz.getConstructor(argumentClass);
            T instance = (T)clazzConstructor.newInstance(argument);
            return instance;
        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
