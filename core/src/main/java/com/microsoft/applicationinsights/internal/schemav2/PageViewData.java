/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Data contract class PageViewData.
 */
public class PageViewData extends EventData {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String PAGE_VIEW_ENVELOPE_NAME = "Microsoft.ApplicationInsights.PageView";

    /**
     * Base Type for this telemetry.
     */
    public static final String PAGE_VIEW_BASE_TYPE = "Microsoft.ApplicationInsights.PageViewData";

    /**
     * Backing field for property Url.
     */
    private String url;

    /**
     * Backing field for property Duration.
     */
    private long duration;

    /**
     * Initializes a new instance of the class.
     */
    public PageViewData()
    {
        this.InitializeFields();
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public Long getDuration() {
        return this.duration;
    }

    public void setDuration(Long value) {
        this.duration = value;
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("url", url);
        writer.write("duration", duration);
    }

    @Override
    public String getEnvelopName() {
        return PAGE_VIEW_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return PAGE_VIEW_BASE_TYPE;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (!(other instanceof PageViewData)) {
            return false;
        }

        PageViewData that = (PageViewData)other;
        return this.url == null ? that.getUrl() == null : this.url.equals(that.getUrl()) &&
               this.duration == that.getDuration() &&
               this.getProperties().equals(that.getProperties());
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return hash * 89 + new HashCodeBuilder(17, 31).
                append(duration).
                append(getProperties()).
                toHashCode();
    }

    protected void InitializeFields() {
    }
}
