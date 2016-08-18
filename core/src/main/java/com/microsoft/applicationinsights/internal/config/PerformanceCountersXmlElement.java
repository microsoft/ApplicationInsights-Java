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

import javax.xml.bind.annotation.*;
import java.util.ArrayList;

/**
 * Created by gupele on 3/15/2015.
 */
@XmlRootElement(name="PerformanceCounters")
public class PerformanceCountersXmlElement {
    private boolean useBuiltIn = true;
    private long cadenceInSec = 60;
//    private WindowsPerformanceCountersXmlElement windowsPCs;

    private ArrayList<JmxXmlElement> jmxXmlElements;
    private ArrayList<WindowsPerformanceCounterXmlElement> windowsPCs;

    public ArrayList<JmxXmlElement> getJmxXmlElements() {
        return jmxXmlElements;
    }

    @XmlElementWrapper(name="Jmx")
    @XmlElement(name="Add")
    public void setJmxXmlElements(ArrayList<JmxXmlElement> jmxXmlElements) {
        this.jmxXmlElements = jmxXmlElements;
    }

    public boolean isUseBuiltIn() {
        return useBuiltIn;
    }

    @XmlElement(name="UseBuiltIn")
    public void setUseBuiltIn(boolean useBuiltIn) {
        this.useBuiltIn = useBuiltIn;
    }

    public ArrayList<WindowsPerformanceCounterXmlElement> getWindowsPCs() {
        return windowsPCs;
    }

    @XmlElementWrapper(name="Windows")
    @XmlElement(name="Add")
    public void setWindowsPCs(ArrayList<WindowsPerformanceCounterXmlElement> windowsPCs) {
        this.windowsPCs = windowsPCs;
    }

    public long getCadenceInSec() {
        return cadenceInSec;
    }

    @XmlAttribute
    public void setCadenceInSec(long cadenceInSec) {
        this.cadenceInSec = cadenceInSec;
    }
}
