package com.microsoft.applicationinsights.internal.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Created by gupele on 3/15/2015.
 */
@XmlRootElement(name="Jmx")
public class JmxListXmlElement {
    private ArrayList<JmxXmlElement> jmx;

    public ArrayList<JmxXmlElement> getJmx() {
        return jmx;
    }

    @XmlElement(name="Add")
    public void setJmx(ArrayList<JmxXmlElement> jmx) {
        this.jmx = jmx;
    }
}
