package com.microsoft.applicationinsights.internal.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
/**
 * Created by gupele on 3/15/2015.
 */
public class JmxDataFetcher {
    public static Map<String, Collection<Object>> fetch(String objectName, Collection<JmxAttributeData> attributes) throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objects = server.queryNames(new ObjectName(objectName), null);

        Map<String, Collection<Object>> result = new HashMap<String, Collection<Object>>();
        for (JmxAttributeData attribute : attributes) {
            Collection<Object> resultForAttribute = fetch(server, objects, attribute.name);
            result.put(attribute.displayName, resultForAttribute);
        }

        return result;
    }

    public static Collection<Object> fetch(String objectName, String attribute) throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objects = server.queryNames(new ObjectName(objectName), null);

        Collection<Object> result = fetch(server, objects, attribute);

        return result;
    }

    public static Collection<Object> fetch(MBeanServer server, Set<ObjectName> objects, String attribute) throws Exception {
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
