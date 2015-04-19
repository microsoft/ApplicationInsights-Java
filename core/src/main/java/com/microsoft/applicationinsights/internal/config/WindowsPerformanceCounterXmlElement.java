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
