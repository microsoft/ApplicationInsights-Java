package com.microsoft.applicationinsights.internal.jmx;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * A utility class that knows how to fetch JMX data.
 *
 * Created by gupele on 3/15/2015.
 */
public class JmxDataFetcher {
    /**
     * Gets an object name and its attributes to fetch and will return the data.
     * @param objectName The object name to search.
     * @param attributes The attributes that 'belong' to the object name.
     * @return A map that represent each attribute: the key is the displayed name for that attribute
     * and the value is a list of values found
     * @throws Exception In case the object name is not found.
     */
    public static Map<String, Collection<Object>> fetch(String objectName, Collection<JmxAttributeData> attributes) throws Exception {
        Map<String, Collection<Object>> result = new HashMap<String, Collection<Object>>();

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objects = server.queryNames(new ObjectName(objectName), null);
        if (objects.isEmpty()) {
            String errorMsg = String.format("Cannot find object name '%s'", objectName);
            throw new IllegalArgumentException(errorMsg);
        }

        for (JmxAttributeData attribute : attributes) {
            try {
                Collection<Object> resultForAttribute = fetch(server, objects, attribute.name);
                result.put(attribute.displayName, resultForAttribute);
            } catch (Exception e) {
                InternalLogger.INSTANCE.error("Failed to fetch JMX object '%s' with attribute '%s': '%s'", objectName, attribute.name, e.getMessage());
            }
        }

        return result;
    }

    private static Collection<Object> fetch(MBeanServer server, Set<ObjectName> objects, String attribute) throws Exception {
        ArrayList<Object> result = new ArrayList<Object>();
        for (ObjectName object : objects) {
            Object obj = server.getAttribute(object, attribute);
            if (obj != null) {
                result.add(obj);
            }
        }

        return result;
    }

    private JmxDataFetcher() {
    }
}
