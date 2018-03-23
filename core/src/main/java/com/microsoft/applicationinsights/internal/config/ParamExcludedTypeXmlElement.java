package com.microsoft.applicationinsights.internal.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * This is the class for binding the xml array list of {@code <ExcludedTypes>}
 */
public class ParamExcludedTypeXmlElement {

    public List<String> getExcludedType() {
        return excludedType;
    }

    @XmlElement(name = "ExcludedType")
    public void setExcludedType(List<String> excludedType) {
        this.excludedType = excludedType;
    }

    private List<String> excludedType;

}
