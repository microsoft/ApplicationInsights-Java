package com.microsoft.applicationinsights.datacontracts;

import java.io.IOException;

import com.microsoft.applicationinsights.extensibility.model.DataPoint;
import com.microsoft.applicationinsights.extensibility.model.MetricData;
import com.microsoft.applicationinsights.util.LocalStringsUtils;
import com.microsoft.applicationinsights.util.MapUtil;

import com.google.common.base.Strings;

/**
 * Telemetry used to track events.
 */
public class MetricTelemetry extends BaseTelemetry
{
    private final MetricData data;
    private final DataPoint  metric;

    public MetricTelemetry()
    {
        super();
        this.data = new MetricData();
        this.metric = new DataPoint();
        initialize(this.data.getProperties());
        this.data.getMetrics().add(this.metric);
    }

    public MetricTelemetry(String name, double value)
    {
        this();
        this.setName(name);
        this.metric.setValue(value);
    }

    public String getName()
    {
        return this.metric.getName();
    }

    public void setName(String name)
    {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The metric name cannot be null or empty");
        }
        this.metric.setName(name);
    }

    public double getValue() { return this.metric.getValue(); }

    public void setValue(double value) { this.metric.setValue(value); }

    public Integer getCount() { return this.metric.getCount();}

    public void setCount(Integer count) { this.metric.setCount(count); updateKind();}

    public Double getMin() { return this.metric.getMin();}

    public void setMin(Double value) { this.metric.setMin(value); updateKind();}

    public Double getMax() { return this.metric.getMax();}

    public void setMax(Double value) { this.metric.setMax(value); updateKind();}

    public Double getStandardDeviation() { return this.metric.getStdDev();}

    public void setStandardDeviation(Double value) { this.metric.setStdDev(value); updateKind();}

    @Override
    public void sanitize()
    {
        this.metric.setName(LocalStringsUtils.sanitize(this.metric.getName(), 1024));
        MapUtil.sanitizeProperties(this.getProperties());
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException
    {
        writer.writeStartObject();

        writer.writeProperty("ver", 1);
        writer.writeProperty("name", "Microsoft.ApplicationInsights.Metric");
        writer.writeProperty("time", this.getTimestamp());

        getContext().serialize(writer);

        writer.writePropertyName("data");

        {
            writer.writeStartObject();

            writer.writeProperty("type", "Microsoft.ApplicationInsights.MetricData");
            writer.writePropertyName("item");

            {
                writer.writeStartObject();
                writer.writeProperty("ver", this.data.getVer());
                writer.writePropertyName("metrics");

                {
                    writer.writeStartArray();
                    writer.writeStartObject();
                    writer.writeProperty("name", LocalStringsUtils.populateRequiredStringWithNullValue(this.metric.getName(), "name", MetricTelemetry.class.getName()));
                    writer.writeProperty("kind", this.metric.getKind());
                    writer.writeProperty("value", this.metric.getValue());
                    writer.writeProperty("count", this.metric.getCount());
                    writer.writeProperty("min", this.metric.getMin());
                    writer.writeProperty("max", this.metric.getMax());
                    writer.writeProperty("stdDev", this.metric.getStdDev());
                    writer.writeEndObject();
                    writer.writeEndArray();
                }

                writer.writeProperty("properties", this.data.getProperties());

                writer.writeEndObject();
            }

            writer.writeEndObject();
        }

        writer.writeEndObject();
    }

    private void updateKind()
    {
        boolean isAggregation =
            (metric.getCount() != null) ||
            (metric.getMin() != null) ||
            (metric.getMax() != null) ||
            (metric.getStdDev() != null);

        if ((metric.getCount() != null) && metric.getCount() == 1)
            // Singular data point. This is not an aggregation.
            isAggregation = false;

        this.metric.setKind(isAggregation ? "Aggregation" : "Measurement");
    }
}
