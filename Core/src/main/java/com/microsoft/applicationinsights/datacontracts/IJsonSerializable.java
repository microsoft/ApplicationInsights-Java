package com.microsoft.applicationinsights.datacontracts;

import java.io.IOException;

/**
 * Created by gupele on 12/24/2014.
 */
public interface IJsonSerializable {
    void serialize(JsonTelemetryDataSerializer serializer) throws IOException;
}
