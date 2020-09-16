package com.microsoft.applicationinsights.internal.config;

import java.util.List;

/**
 * This is the class for binding the xml array list of {@code <ExcludedTypes>}
 */
public class ParamExcludedTypeXmlElement {

    public List<String> getExcludedType() {
        return excludedType;
    }

    public void setExcludedType(List<String> excludedType) {
        this.excludedType = excludedType;
    }

    private List<String> excludedType;
}
