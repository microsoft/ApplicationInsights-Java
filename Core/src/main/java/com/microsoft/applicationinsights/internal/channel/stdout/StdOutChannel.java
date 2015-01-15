package com.microsoft.applicationinsights.internal.channel.stdout;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

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

    public StdOutChannel(TelemetryConfiguration configuration) {
    }

    @Override
    public void send(Telemetry item) {
        try {
            StringWriter writer = new StringWriter();
            item.serialize(new JsonTelemetryDataSerializer(writer));
            InternalLogger.INSTANCE.log("StdOutChannel, TELEMETRY: %s", writer.toString());
        } catch (IOException ioe) {
        }
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
    }
}
