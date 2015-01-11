package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

/**
 * Data contract class RemoteDependencyData.
 */
public class RemoteDependencyData extends Domain {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String REMOTE_ENVELOPE_NAME = "Microsoft.ApplicationInsights.RemoteDependency";

    /**
     * Base Type for this telemetry.
     */
    public static final String REMOTE_BASE_TYPE = "Microsoft.ApplicationInsights.RemoteDependencyData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

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
     * Backing field for property DependencyKind.
     */
    private DependencyKind dependencyKind = DependencyKind.Undefind;

    /**
     * Backing field for property Success.
     */
    private Boolean success = true;

    /**
     * Backing field for property Async.
     */
    private Boolean async;

    /**
     * Backing field for property DependencySource.
     */
    private DependencySourceType dependencySource = DependencySourceType.Undefined;

    /**
     * Backing field for property Properties.
     */
    private HashMap<String, String> properties;

    /**
     * Initializes a new instance of the <see cref="RemoteDependencyData"/> class.
     */
    public RemoteDependencyData()
    {
        this.InitializeFields();
    }

    /**
     * Gets the Ver property.
     */
    public int getVer() {
        return this.ver;
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
     * Gets the DependencyKind property.
     */
    public DependencyKind getDependencyKind() {
        return this.dependencyKind;
    }

    /**
     * Sets the DependencyKind property.
     */
    public void setDependencyKind(DependencyKind value) {
        this.dependencyKind = value;
    }

    /**
     * Gets the Success property.
     */
    public Boolean getSuccess() {
        return this.success;
    }

    /**
     * Sets the Success property.
     */
    public void setSuccess(Boolean value) {
        this.success = value;
    }

    /**
     * Gets the Async property.
     */
    public Boolean getAsync() {
        return this.async;
    }

    /**
     * Sets the Async property.
     */
    public void setAsync(Boolean value) {
        this.async = value;
    }

    /**
     * Gets the DependencySource property.
     */
    public DependencySourceType getDependencySource() {
        return this.dependencySource;
    }

    /**
     * Sets the DependencySource property.
     */
    public void setDependencySource(DependencySourceType value) {
        this.dependencySource = value;
    }

    /**
     * Gets the Properties property.
     */
    public HashMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new HashMap<String, String>();
        }
        return this.properties;
    }

    /**
     * Sets the Properties property.
     */
    public void setProperties(HashMap<String, String> value) {
        this.properties = value;
    }


    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("name", name);

        if (!DataPointType.Measurement.equals(kind)) {
            writer.write("kind", kind.getValue());
        }

        writer.write("value", value);
        writer.write("count", count);
        writer.write("min", min);
        writer.write("max", max);
        writer.write("stdDev", stdDev);
        writer.write("dependencyKind", dependencyKind.getValue());
        writer.write("success", success);
        writer.write("async", async);

        if (!DependencySourceType.Undefined.equals(dependencyKind)) {
            writer.write("dependencySource", dependencySource.getValue());
        }

        writer.write("properties", properties);
    }

    public String getEnvelopName() {
        return REMOTE_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return REMOTE_BASE_TYPE;
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }
}
