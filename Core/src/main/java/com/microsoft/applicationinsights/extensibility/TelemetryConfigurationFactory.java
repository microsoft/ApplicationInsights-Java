package com.microsoft.applicationinsights.extensibility;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.microsoft.applicationinsights.channel.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.implementation.DeviceInfoContextInitializer;
import com.microsoft.applicationinsights.implementation.PropertiesContextInitializer;
import com.microsoft.applicationinsights.implementation.SdkVersionContextInitializer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Strings;

/**
 * Initializer class for configuration instances.
 */
public enum TelemetryConfigurationFactory {
    INSTANCE;

    private final static String CONTEXT_INITIALIZERS_SECTION = "ContextInitializers";
    private final static String TELEMETRY_INITIALIZERS_SECTION = "TelemetryInitializers";
    private final static String INITIALIZERS_ADD = "Add";
    private final static String INITIALIZERS_TYPE = "Type";
    private final static String CONFIG_FILE_NAME = "ApplicationInsights.xml";

    /**
     * Inner helper class that knows how to parse and work with the configuration file
     */
    public static final class ConfigParser {
        private Document doc;

        public boolean parse(String fileName) {
            try {
                ClassLoader classLoader = TelemetryConfigurationFactory.class.getClassLoader();

                InputStream inputStream = classLoader.getResourceAsStream(fileName);
                if (inputStream == null) {
                    return false;
                }

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                doc = db.parse(inputStream);
                doc.getDocumentElement().normalize();

                return true;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        public String getValue(String item) {
            NodeList nodeList = doc.getElementsByTagName(item);
            for (int counter = 0; counter < nodeList.getLength(); ++counter) {
                Node elementNode = nodeList.item(counter);
                if (elementNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                return ((Element)elementNode).getTextContent();
            }

            return null;
        }

        public Collection<String> getList(String listName, String listItem, String attribute) {
            List<String> result = new ArrayList<String>();

            NodeList nodeList = doc.getElementsByTagName(listName);
            for (int counter = 0; counter < nodeList.getLength(); ++counter) {
                Node elementNode = nodeList.item(counter);
                if (elementNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                NodeList items = ((Element)elementNode).getElementsByTagName(listItem);
                for (int i = 0; i < items.getLength(); ++i) {
                    Node elementItem = items.item(i);
                    if (elementItem.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    String attr = ((Element)elementItem).getAttribute(attribute);
                    if (attr != null) {
                        result.add(attr);
                    }
                }
            }

            return result;
        }
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
    public final void Initialize(TelemetryConfiguration configuration) {
        ConfigParser parser = new ConfigParser();
        if (!parser.parse(CONFIG_FILE_NAME)) {
            return;
        }

        if (!setInstrumentationKey(parser, configuration)) {
            return;
        }

        setDeveloperMode(parser, configuration);

        if (!setChannel(parser, configuration)) {
            return;
        }

        setTrackingDisabledMode(parser, configuration);

        setContextInitializers(parser, configuration);
        setTelemetryInitializers(parser, configuration);
    }

    private void setTrackingDisabledMode(ConfigParser parser, TelemetryConfiguration configuration) {
        configuration.setTrackingIsDisabled(fetchBooleanValue(parser, "DisableTelemetry", false));
    }

    private void setDeveloperMode(ConfigParser parser, TelemetryConfiguration configuration) {
        configuration.setDeveloperMode(fetchBooleanValue(parser, "DeveloperMode", false));
    }

    private boolean setChannel(ConfigParser parser, TelemetryConfiguration configuration) {
        String channelName = parser.getValue("Channel");

        if (channelName != null) {
            TelemetryChannel channel = createInstance(channelName, TelemetryChannel.class);
            if (channel != null) {
                configuration.setChannel(channel);
                return true;
            }
        }

        try {
            configuration.setChannel(new InProcessTelemetryChannel());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean setInstrumentationKey(ConfigParser parser, TelemetryConfiguration configuration) {
        String iKey = parser.getValue("InstrumentationKey");
        if (iKey == null) {
            return false;
        }

        iKey = iKey.trim();
        if ("".equals(iKey)) {
            return false;
        }

        configuration.setInstrumentationKey(iKey);

        return true;
    }

    private void setContextInitializers(ConfigParser parser, TelemetryConfiguration configuration) {
        List<ContextInitializer> initializerList = configuration.getContextInitializers();

        // Guy: to keep with prev version. A few will probably be moved to the configuration
        initializerList.add(new SdkVersionContextInitializer());
        initializerList.add(new DeviceInfoContextInitializer());
        initializerList.add(new PropertiesContextInitializer());

        setInitializers(ContextInitializer.class, parser, CONTEXT_INITIALIZERS_SECTION, INITIALIZERS_ADD, initializerList);
    }

    private void setTelemetryInitializers(ConfigParser parser, TelemetryConfiguration configuration) {
        List<TelemetryInitializer> initializerList = configuration.getTelemetryInitializers();
        setInitializers(TelemetryInitializer.class, parser, TELEMETRY_INITIALIZERS_SECTION, INITIALIZERS_ADD, initializerList);
    }

    private boolean fetchBooleanValue(ConfigParser parser, String name, boolean defaultValue) {
        boolean result = defaultValue;

        String value = parser.getValue(name);
        if (!Strings.isNullOrEmpty(value)) {
            result = Boolean.valueOf(value);
        }

        return result;
    }

    private <T> void setInitializers(
            Class<T> clazz,
            ConfigParser parser,
            String sectionName,
            String itemName,
            List<T> list) {
        Collection<String> classNames = parser.getList(sectionName, itemName, INITIALIZERS_TYPE);
        for (String className : classNames) {
            T initializer = createInstance(className, clazz);
            if (initializer != null) {
                list.add(initializer);
            }
        }
    }

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
}
