package com.microsoft.applicationinsights.implementation.schemav2;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import java.io.IOException;
import java.util.Map;

/**
 * Created by gupele on 12/25/2014.
 */
public class RemoteDependencyDataWrapper implements JsonSerializable {
    private final RemoteDependencyData item;

    public RemoteDependencyDataWrapper() {
        item = new RemoteDependencyData();
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("type", "Microsoft.ApplicationInsights.RemoteDependencyData");
        writer.write("item", item);
    }

    public Map<String, String> getProperties() {
        return item.getProperties();
    }

    public String getName() {
        return item.getName();
    }

    public void setName(String name) {
        item.setName(name);
    }

    public DependencyKind getDependencyKind() {
        return item.getDependencyKind();
    }

    public void setDependencyKind(DependencyKind dependencyKind) {
        item.setDependencyKind(dependencyKind);
    }

    public double getValue() {
        return item.getValue();
    }

    public void setValue(double value) {
        item.setValue(value);
    }

    public Integer getCount() {
        return item.getCount();
    }

    public void setCount(Integer count) {
        item.setCount(count);
    }
}
