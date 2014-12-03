package com.microsoft.applicationinsights.datacontracts;

import com.microsoft.applicationinsights.extensibility.model.DependencyKind;
import com.microsoft.applicationinsights.extensibility.model.RddSourceType;
import com.microsoft.applicationinsights.extensibility.model.RemoteDependencyData;
import com.microsoft.applicationinsights.util.MapUtil;
import com.microsoft.applicationinsights.util.StringUtil;

import java.io.IOException;

/**
 * Telemetry used to track events.
 */
public class RemoteDependencyTelemetry extends BaseTelemetry
{
    private final RemoteDependencyData data;

    public RemoteDependencyTelemetry()
    {
        super();
        this.data = new RemoteDependencyData();
        initialize(this.data.getProperties());
    }

    public RemoteDependencyTelemetry(String name)
    {
        this();
        this.setName(name);
    }

    public String getName()
    {
        return this.data.getName();
    }

    public void setName(String name)
    {
        if (StringUtil.isNullOrEmpty(name))
            throw new IllegalArgumentException("The event name cannot be null or empty");
        this.data.setName(name);
    }

    public DependencyKind getDependencyKind()
    {
        return this.data.getDependencyKind();
    }

    public void setDependencyKind(DependencyKind dependencyKind)
    {
        this.data.setDependencyKind(dependencyKind);
    }

    public double getValue()
    {
        return this.data.getValue();
    }

    public void setValue(double value)
    {
        this.data.setValue(value);
    }

    public Integer getCount()
    {
        return this.data.getCount();
    }

    public void setCount(Integer count)
    {
        this.data.setCount(count);
    }

    public Boolean getSuccess()
    {
        return this.data.getSuccess();
    }

    public void setSuccess(Boolean success)
    {
        this.data.setSuccess(success);
    }

    public Boolean getAsync()
    {
        return this.data.getAsync();
    }

    public void setAsync(Boolean async)
    {
        this.data.setAsync(async);
    }

    public RddSourceType getRddSource()
    {
        return this.data.getRddSource();
    }

    public void setRddSource(RddSourceType rddSource)
    {
        this.data.setRddSource(rddSource);
    }

    @Override
    public void sanitize()
    {
        this.data.setName(StringUtil.sanitize(this.data.getName(), 1024));
        MapUtil.sanitizeProperties(this.getProperties());
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException
    {
        writer.writeStartObject();

        writer.writeProperty("ver", 1);
        writer.writeProperty("name", "Microsoft.ApplicationInsights.RemoteDependency");
        writer.writeProperty("time", this.getTimestamp());

        getContext().serialize(writer);

        writer.writePropertyName("data");

        {
            writer.writeStartObject();

            writer.writeProperty("type", "Microsoft.ApplicationInsights.RemoteDependencyData");

            writer.writePropertyName("item");
            {
                writer.writeStartObject();
                writer.writeProperty("ver", this.data.getVer());
                writer.writeProperty("name", this.data.getName());
                writer.writeProperty("kind", this.data.getKind());
                writer.writeProperty("value", this.data.getValue());
                writer.writeProperty("count", this.data.getCount());
                writer.writeProperty("dependencyKind", this.data.getDependencyKind().toString());
                writer.writeProperty("success", this.data.getSuccess());
                writer.writeProperty("async", this.data.getAsync());
                writer.writeProperty("source", this.data.getRddSource().getValue());
                writer.writeProperty("properties", this.data.getProperties());
                writer.writeEndObject();
            }

            writer.writeEndObject();
        }

        writer.writeEndObject();
    }
}
