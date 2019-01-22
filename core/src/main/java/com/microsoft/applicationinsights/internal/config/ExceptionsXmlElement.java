package com.microsoft.applicationinsights.internal.config;

import javax.xml.bind.annotation.XmlElement;

public class ExceptionsXmlElement {

    private Integer maxStackSize;
    private Integer maxTraceLength;

    public Integer getMaxStackSize() {
        return maxStackSize;
    }

    @XmlElement(name="MaxStackSize")
    public void setMaxStackSize(Integer maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    public Integer getMaxTraceLength() {
        return maxTraceLength;
    }

    @XmlElement(name="MaxTraceLength")
    public void setMaxTraceLength(Integer maxTraceLength) {
        this.maxTraceLength = maxTraceLength;
    }
}
