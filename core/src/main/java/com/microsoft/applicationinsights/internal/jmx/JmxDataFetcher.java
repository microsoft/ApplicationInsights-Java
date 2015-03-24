package com.microsoft.applicationinsights.internal.jmx;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import java.lang.management.ManagementFactory;

/**
 * A utility class that knows how to fetch JMX data.
 *
 * Created by gupele on 3/15/2015.
 */
public class JmxDataFetcher {
    private static final String COMPOSITE_ATTRIBUTE_TYPE = "COMPOSITE";
    private static final String TABULAR_ATTRIBUTE_TYPE = "TABULAR";

    enum AttributeType {
        TABULAR,
        COMPOSITE,
        REGULAR
    }

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
                Collection<Object> resultForAttribute = fetch(server, objects, attribute.name, attribute.type);
                result.put(attribute.displayName, resultForAttribute);
            } catch (Exception e) {
                InternalLogger.INSTANCE.error("Failed to fetch JMX object '%s' with attribute '%s': '%s'", objectName, attribute.name, e.getMessage());
                throw e;
            }
        }

        return result;
    }

    private static Collection<Object> fetch(MBeanServer server, Set<ObjectName> objects, String attributeName, String attributeType) throws Exception {
        ArrayList<Object> result = new ArrayList<Object>();

        String attr = attributeName;
        String[] inners = null;

        AttributeType innerAttributeType = AttributeType.REGULAR;
        if (COMPOSITE_ATTRIBUTE_TYPE.equals(attributeType)) {
            innerAttributeType = AttributeType.COMPOSITE;
        } else if (TABULAR_ATTRIBUTE_TYPE.equals(attributeType)) {
            innerAttributeType = AttributeType.TABULAR;
        }

        if (innerAttributeType != AttributeType.REGULAR) {
            inners = attributeName.split("\\.");
        }

        for (ObjectName object : objects) {

            Object obj;

            if (innerAttributeType != AttributeType.REGULAR) {
                obj = server.getAttribute(object, inners[0]);

                javax.management.openmbean.CompositeDataSupport compositeData = null;
                if (innerAttributeType == AttributeType.TABULAR) {
                    javax.management.openmbean.TabularDataSupport tabularData = (javax.management.openmbean.TabularDataSupport)obj;
                    compositeData = (CompositeDataSupport) tabularData.get(inners[1]);
                    obj = compositeData.get(inners[2]);
                } else {
                    compositeData = (javax.management.openmbean.CompositeDataSupport)obj;
                    obj = compositeData.get(inners[1]);
                }
            } else {
                obj = server.getAttribute(object, attr);
            }
            if (obj != null) {
                result.add(obj);
            }
        }

        return result;
    }

    private JmxDataFetcher() {
    }
}
