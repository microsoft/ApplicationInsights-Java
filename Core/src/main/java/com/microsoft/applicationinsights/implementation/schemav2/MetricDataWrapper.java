package com.microsoft.applicationinsights.implementation.schemav2;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gupele on 12/25/2014.
 */
public class MetricDataWrapper implements JsonSerializable {
    private final MetricData item;

    public MetricDataWrapper() {
        item = new MetricData();
    }

    /**
     * Gets the Ver property.
     */
    public int getVer() {
        return item.getVer();
    }

    /**
     * Sets the Ver property.
     */
    public void setVer(int value) {
        item.setVer(value);
    }

    /**
     * Gets the Metrics property.
     */
    public List<DataPoint> getMetrics() {
        return item.getMetrics();
    }

    /**
     * Sets the Metrics property.
     */
    public void setMetrics(ArrayList<DataPoint> value) {
        item.setMetrics(value);
    }

    /**
     * Gets the Properties property.
     */
    public HashMap<String, String> getProperties() {
        return item.getProperties();
    }

    /**
     * Sets the Properties property.
     */
    public void setProperties(HashMap<String, String> value) {
        item.setProperties(value);
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("type", "Microsoft.ApplicationInsights.MetricData");
        writer.write("item", item);
    }
}
