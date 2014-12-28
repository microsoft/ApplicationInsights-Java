package com.microsoft.applicationinsights.implementation.schemav2;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import java.io.IOException;

/**
 * Created by gupele on 12/24/2014.
 */
public class EventDataWrapper implements JsonSerializable {
    private final EventData item;

    public EventDataWrapper() {
        item = new EventData();
    }

    public EventData getItem() {
        return item;
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        writer.write("type", "Microsoft.ApplicationInsights.EventData");
        writer.write("item", item);
    }
}
