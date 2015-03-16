package com.microsoft.applicationinsights.internal.jmx;

/**
 * Represents JMX data of an Attribute
 * The display name
 * The name of the attribute
 * The type of the attribute
 *
 * Created by gupele on 3/15/2015.
 */
public final class JmxAttributeData {
    public final String displayName;
    public final String name;
    public final String type;

    public JmxAttributeData(String displayName, String name) {
        this(displayName, name, null);
    }

    public JmxAttributeData(String displayName, String name, String type) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
    }
}
