package com.microsoft.applicationinsights.channel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * The class is an implementation of the {@see TelemetrySerializer}
 * where the {@see Telemetry} instances are compressed by Gzip after converted to Json format
 *
 * Created by gupele on 12/17/2014.
 */
final class GzipTelemetrySerializer implements TelemetrySerializer {
    private final static String GZIP_WEB_CONTENT_TYPE = "application/x-json-stream";
    private final static String GZIP_WEB_ENCODING_TYPE = "gzip";

    private final byte[] newlineString;

    public GzipTelemetrySerializer() {
        this.newlineString = System.getProperty("line.separator").getBytes();
    }

    @Override
    public Optional<Transmission> serialize(Collection<Telemetry> telemetries) {
        Preconditions.checkNotNull(telemetries, "telemetries must be non-null value");
        Preconditions.checkArgument(!telemetries.isEmpty(), "telemetries: One or more telemetry item is expected");

        Transmission result = null;
        boolean succeeded = false;
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            try {
                GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);

                try {
                    serializeAndCompress(zipStream, telemetries);
                    succeeded = true;
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Throwable t) {
                } finally {
                    zipStream.close();
                }
            } finally {
                byteStream.close();

                // The creation of the result must be done after the 'zipStream' is closed
                if (succeeded) {
                    result = new Transmission(byteStream.toByteArray(), GZIP_WEB_CONTENT_TYPE, GZIP_WEB_ENCODING_TYPE);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return Optional.fromNullable(result);
    }

    private void serializeAndCompress(GZIPOutputStream zipStream, Collection<Telemetry> telemetries) throws IOException {
        int counter = 0;
        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);

        // The format is:
        // 1. Telemetry is written in Json
        // 2. Separate each Telemetry by newline
        // 3. Compress the entire data by using Gzip
        for (Telemetry telemetry : telemetries) {

            if (counter != 0) {
                zipStream.write(newlineString);
            }

            ++counter;

            telemetry.serialize(jsonWriter);
            jsonWriter.close();

            String asJson = writer.toString();
            zipStream.write(asJson.getBytes());

            if (counter < telemetries.size()) {
                writer.getBuffer().setLength(0);
                jsonWriter.reset(writer);
            }
        }
    }
}
