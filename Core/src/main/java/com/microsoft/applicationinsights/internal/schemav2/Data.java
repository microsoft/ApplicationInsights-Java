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
     * Initializes a new instance of the <see cref="Data{TDomain}"/> class.
     */
    public Data() {
        this(null);
    }

    /**
     * Initializes a new instance of the <see cref="Data{TDomain}"/> class with base data
     */
    public Data(TDomain baseData) {
        super();
        setBaseData(baseData);
        this.InitializeFields();
    }

    /**
     * Gets the BaseData property.
     */
    public TDomain getBaseData() {
        return this.baseData;
    }

    /**
     * Sets the BaseData property.
     */
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
