/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.channel.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import com.microsoft.applicationinsights.internal.channel.TelemetrySerializer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * The class is an implementation of the {@link TelemetrySerializer}
 * where the {@link Telemetry} instances are compressed by Gzip after converted to Json format
 *
 * Created by gupele on 12/17/2014.
 */
public final class GzipTelemetrySerializer implements TelemetrySerializer {
    private final static String GZIP_WEB_CONTENT_TYPE = "application/x-json-stream";
    private final static String GZIP_WEB_ENCODING_TYPE = "gzip";

    private final byte[] newlineString;

    public GzipTelemetrySerializer() {
        this.newlineString = System.getProperty("line.separator").getBytes();
    }

    @Override
    public Optional<Transmission> serialize(Collection<String> telemetries) {
        Preconditions.checkNotNull(telemetries, "telemetries must be non-null value");
        Preconditions.checkArgument(!telemetries.isEmpty(), "telemetries: One or more telemetry item is expected");

        Transmission result = null;
        boolean succeeded = false;
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            try {
                GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);

                try {
                    succeeded = compress(zipStream, telemetries);
                } catch (Exception e) {
                    InternalLogger.INSTANCE.error("Failed to serialize , exception: %s", e.getMessage());
                } catch (Throwable t) {
                    InternalLogger.INSTANCE.error("Failed to serialize, unknown exception: %s", t.getMessage());
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
            InternalLogger.INSTANCE.error("Failed to serialize , exception: %s", e.getMessage());
        }

        return Optional.fromNullable(result);
    }

    private boolean compress(GZIPOutputStream zipStream, Collection<String> telemetries) throws IOException {
        int counter = 0;
        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(writer);

        // The format is:
        // 1. Separate each Telemetry by newline
        // 2. Compress the entire data by using Gzip
        for (String telemetry : telemetries) {

            if (counter != 0) {
                zipStream.write(newlineString);
            }

            try {
                zipStream.write(telemetry.getBytes());
                ++counter;
            } catch (Exception e) {
                InternalLogger.INSTANCE.error("Failed to serialize , exception: %s", e.getMessage());
            }

            if (counter < telemetries.size()) {
                writer.getBuffer().setLength(0);
                jsonWriter.reset(writer);
            }
        }

        return counter > 0;
    }
}
