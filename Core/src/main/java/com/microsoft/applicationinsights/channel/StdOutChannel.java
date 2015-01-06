package com.microsoft.applicationinsights.channel;

import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.extensibility.TelemetryClientConfiguration;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

/**
 * A telemetry channel routing information to stdout.
 */
public class StdOutChannel implements TelemetryChannel
{
    @Override
    public boolean isDeveloperMode() {
        return false;
    }

    @Override
    public void setDeveloperMode(boolean value) {
        // Just ignore it.
    }

    public StdOutChannel() {
        this(null);
    }

    public StdOutChannel(TelemetryClientConfiguration configuration) {
    }

    @Override
    public void send(Telemetry item) {
        try {
            StringWriter writer = new StringWriter();
//            item.serialize(new JsonWriter(writer));
            item.serialize(new JsonTelemetryDataSerializer(writer));
            System.out.println("TELEMETRY: " + writer.toString());
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
    }
}
