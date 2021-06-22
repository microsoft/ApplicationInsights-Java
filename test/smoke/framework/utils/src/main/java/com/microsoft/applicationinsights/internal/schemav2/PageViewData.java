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
/*
 * Generated from PageViewData.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;

import com.microsoft.applicationinsights.telemetry.Duration;

/**
 * Data contract class PageViewData.
 */
public class PageViewData extends EventData
{
    /**
     * Backing field for property Url.
     */
    private String url;

    /**
     * Backing field for property Duration.
     */
    private Duration duration = new Duration(0);

    /**
     * Initializes a new instance of the PageViewData class.
     */
    public PageViewData()
    {
    }

    /**
     * Gets the Url property.
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Sets the Url property.
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the Duration property.
     */
    public Duration getDuration() {
        return this.duration;
    }

    /**
     * Sets the Duration property.
     */
    public void setDuration(Duration value) {
        this.duration = value;
    }
}
