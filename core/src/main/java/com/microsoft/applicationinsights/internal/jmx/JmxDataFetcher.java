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

package com.microsoft.applicationinsights.internal.jmx;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
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
                InternalLogger.INSTANCE.error("Failed to fetch JMX object '%s' with attribute '%s': '%s'", objectName, attribute.name, e.toString());
                throw e;
            }
        }

        return result;
    }

    private static Collection<Object> fetch(MBeanServer server, Set<ObjectName> objects, String attributeName, String attributeType) throws Exception {
        ArrayList<Object> result = new ArrayList<Object>();

        AttributeType innerAttributeType = AttributeType.REGULAR;
        if (COMPOSITE_ATTRIBUTE_TYPE.equals(attributeType)) {
            innerAttributeType = AttributeType.COMPOSITE;
        } else if (TABULAR_ATTRIBUTE_TYPE.equals(attributeType)) {
            innerAttributeType = AttributeType.TABULAR;
        }

        switch (innerAttributeType) {
            case TABULAR:
                fetchTabularObjects(server, objects, attributeName, result);
                break;
            case COMPOSITE:
                fetchCompositeObjects(server, objects, attributeName, result);
                break;
            case REGULAR:
                fetchRegularObjects(server, objects, attributeName, result);
                break;
        }

        return result;
    }

    static void fetchRegularObjects(MBeanServer server, Set<ObjectName> objects, String attributeName, List<Object> result)
            throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException {

        for (ObjectName object : objects) {
            Object obj = server.getAttribute(object, attributeName);
            if (obj != null) {
                result.add(obj);
            } else {
                InternalLogger.INSTANCE.warn("Could not find JMX attribute named '%s' in object '%s'", attributeName, object);
            }
        }
    }

    static void fetchCompositeObjects(MBeanServer server, Set<ObjectName> objects, String attributeName, List<Object> result)
            throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException {

        String[] inners = attributeName.split("\\.");
        for (ObjectName object : objects) {
            CompositeData compositeData = (CompositeData) server.getAttribute(object, inners[0]);
            if (compositeData == null) {
                InternalLogger.INSTANCE.warn("Could not find composite attribute named '%s' for '%s' in object '%s'", inners[0], attributeName, object);
                continue;
            }
            Object obj = compositeData.get(inners[1]);

            if (obj != null) {
                result.add(obj);
            } else {
                InternalLogger.INSTANCE.warn("Could not find composite attribute named '%s' for '%s' in object '%s'", inners[1], attributeName, object);
            }
        }
    }

    static void fetchTabularObjects(MBeanServer server, Set<ObjectName> objects, String attributeName, List<Object> result)
            throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException {

        String[] inners = attributeName.split("\\.");
        for (ObjectName object : objects) {
            TabularData tabularData = (TabularData) server.getAttribute(object, inners[0]);
            if (tabularData == null) {
                InternalLogger.INSTANCE.warn("Could not find tabular attribute named '%s' for '%s' in object '%s'", inners[0], attributeName, object);
                continue;
            }

            CompositeData compositeData = tabularData.get(new Object[] { inners[1] });
            if (compositeData == null) {
                InternalLogger.INSTANCE.warn("Could not find tabular attribute named '%s' for '%s' in object '%s'", inners[1], attributeName, object);
                continue;
            }

            Object obj = compositeData.get(inners[2]);
            if (obj != null) {
                result.add(obj);
            } else {
                InternalLogger.INSTANCE.warn("Could not find tabular attribute named '%s' for '%s' in object '%s'", inners[2], attributeName, object);
            }
        }
    }

    private JmxDataFetcher() {
    }
}
