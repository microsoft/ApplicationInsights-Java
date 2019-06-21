package com.microsoft.applicationinsights.internal.config;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * This class is used to bind the xml array list of {@code <IncludeTypes>}
 */
public class ParamIncludedTypeXmlElement {

    public List<String> getIncludedType() {
        return includedType;
    }

    public void setIncludedType(List<String> includedType) {
        this.includedType = includedType;
    }

    @XStreamImplicit(itemFieldName = "IncludedType")
    private List<String> includedType;
}
