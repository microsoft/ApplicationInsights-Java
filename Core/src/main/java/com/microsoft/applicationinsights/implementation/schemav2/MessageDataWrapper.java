package com.microsoft.applicationinsights.implementation.schemav2;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import java.io.IOException;
import java.util.Map;

/**
 * Created by gupele on 12/25/2014.
 */
public class MessageDataWrapper implements JsonSerializable {
    private final MessageData item;

    public MessageDataWrapper() {
        item = new MessageData();
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("type", "Microsoft.ApplicationInsights.MessageData");
        writer.write("item", item);
    }

    public Map<String, String> getProperties() {
        return item.getProperties();
    }

    public void setMessage(String message) {
        item.setMessage(message);
    }

    public String getMessage() {
        return item.getMessage();
    }
}
