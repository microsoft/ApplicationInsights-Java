package com.microsoft.applicationinsights.internal.config;

import java.util.List;

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

    private List<String> includedType;
}
