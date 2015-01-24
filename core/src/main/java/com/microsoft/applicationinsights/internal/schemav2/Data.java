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

/**
 * Data contract class Data.
 */
public class Data<TDomain extends SendableData> extends Base implements SendableData {
    /**
     * Backing field for property BaseData.
     */
    private TDomain baseData;

    /**
     * Initializes a new instance of the class.
     */
    public Data() {
        this(null);
    }

    /**
     * Initializes a new instance of the class with base data
     * @param baseData The data this instance works with.
     */
    public Data(TDomain baseData) {
        super();
        setBaseData(baseData);
        this.InitializeFields();
    }

    public TDomain getBaseData() {
        return this.baseData;
    }

    public void setBaseData(TDomain baseData) {
        this.baseData = baseData;
        if (this.baseData != null) {
            setBaseType(baseData.getBaseTypeName());
        }
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("baseData", baseData);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }

    @Override
    public String getEnvelopName() {
        if (baseData != null) {
            return baseData.getEnvelopName();
        }

        return "";
    }

    @Override
    public String getBaseTypeName() {
        if (baseData != null) {
            return baseData.getBaseTypeName();
        }

        return "";
    }
}
