package com.microsoft.applicationinsights.internal.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by gupele on 3/30/2015.
 */
@XmlRootElement(name="Add")
public class WindowsPerformanceCounterXmlElement {
    private String displayName;
    private String categoryName;
    private String counterName;
    private String instanceName;

    public String getDisplayName() {
        return displayName;
    }

    @XmlAttribute
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    @XmlAttribute
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCounterName() {
        return counterName;
    }

    @XmlAttribute
    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    @XmlAttribute
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
