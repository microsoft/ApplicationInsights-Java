package com.microsoft.applicationinsights.implementation.schemav2;

import java.io.IOException;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class DataPoint.
 */
public final class DataPoint implements JsonSerializable, SendableData {
    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property Kind.
     */
    private DataPointType kind = DataPointType.Measurement;

    /**
     * Backing field for property Value.
     */
    private double value;

    /**
     * Backing field for property Count.
     */
    private Integer count;

    /**
     * Backing field for property Min.
     */
    private Double min;

    /**
     * Backing field for property Max.
     */
    private Double max;

    /**
     * Backing field for property StdDev.
     */
    private Double stdDev;

    /**
     * Initializes a new instance of the <see cref="DataPoint"/> class.
     */
    public DataPoint()
    {
        this.InitializeFields();
    }

    /**
     * Gets the Name property.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the Name property.
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the Kind property.
     */
    public DataPointType getKind() {
        return this.kind;
    }

    /**
     * Sets the Kind property.
     */
    public void setKind(DataPointType value) {
        this.kind = value;
    }

    /**
     * Gets the Value property.
     */
    public double getValue() {
        return this.value;
    }

    /**
     * Sets the Value property.
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Gets the Count property.
     */
    public Integer getCount() {
        return this.count;
    }

    /**
     * Sets the Count property.
     */
    public void setCount(Integer value) {
        this.count = value;
    }

    /**
     * Gets the Min property.
     */
    public Double getMin() {
        return this.min;
    }

    /**
     * Sets the Min property.
     */
    public void setMin(Double value) {
        this.min = value;
    }

    /**
     * Gets the Max property.
     */
    public Double getMax() {
        return this.max;
    }

    /**
     * Sets the Max property.
     */
    public void setMax(Double value) {
        this.max = value;
    }

    /**
     * Gets the StdDev property.
     */
    public Double getStdDev() {
        return this.stdDev;
    }

    /**
     * Sets the StdDev property.
     */
    public void setStdDev(Double value) {
        this.stdDev = value;
    }


    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer should be a nno-null value");

        serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("name", name);

        if (!DataPointType.Measurement.equals(kind)) {
            writer.write("kind", kind.getValue());
        }

        writer.write("value", value);
        writer.write("count", count);
        writer.write("min", min);
        writer.write("max", max);
        writer.write("stdDev", stdDev);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }

    @Override
    public String getEnvelopName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBaseTypeName() {
        throw new UnsupportedOperationException();
    }
}
