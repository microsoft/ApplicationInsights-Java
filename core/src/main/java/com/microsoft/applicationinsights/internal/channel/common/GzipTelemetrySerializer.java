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

import java.io.IOException;
import java.util.Collection;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.channel.TelemetrySerializer;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.squareup.moshi.JsonWriter;
import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class is an implementation of the {@link TelemetrySerializer}
 * where the {@link Telemetry} instances are compressed by Gzip after converted to Json format
 *
 * Created by gupele on 12/17/2014.
 */
public final class GzipTelemetrySerializer implements TelemetrySerializer {

    private static final Logger logger = LoggerFactory.getLogger(GzipTelemetrySerializer.class);

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
            Buffer buffer = new Buffer();

            try {
                GzipSink gzipSink = new GzipSink(buffer);
                BufferedSink bufferedSink = Okio.buffer(gzipSink);

                try {
                    succeeded = compress(bufferedSink, telemetries);
                } catch (Exception e) {
                    logger.error("Failed to serialize , exception: {}", e.toString());
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    try {
                        logger.error("Failed to serialize, unknown exception: {}", t.toString());                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t2) {
                        // chomp
                    }
                } finally {
                    bufferedSink.close();
                }
            } finally {
                // The creation of the result must be done after the 'zipStream' is closed
                if (succeeded) {
                    // TODO further optimize by passing buffer and using okio http
                    result = new Transmission(buffer.readByteArray(), GZIP_WEB_CONTENT_TYPE, GZIP_WEB_ENCODING_TYPE);
                }
                buffer.clear();
            }
        } catch(Exception e) {
            logger.error("Failed to serialize , exception: {}", e.toString());
        }

        return Optional.fromNullable(result);
    }

    public Optional<Transmission> serializeFromStrings(Collection<String> telemetries) {
        Preconditions.checkNotNull(telemetries, "telemetries must be non-null value");
        Preconditions.checkArgument(!telemetries.isEmpty(), "telemetries: One or more telemetry item is expected");

        Transmission result = null;
        boolean succeeded = false;
        try {
            Buffer buffer = new Buffer();

            try {
                GzipSink gzipSink = new GzipSink(buffer);
                BufferedSink bufferedSink = Okio.buffer(gzipSink);

                try {
                    succeeded = compressFromStrings(bufferedSink, telemetries);
                } catch (Exception e) {
                    logger.error("Failed to serialize , exception: {}", e.toString());
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    try {
                        logger.error("Failed to serialize, unknown exception: {}", t.toString());                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t2) {
                        // chomp
                    }
                } finally {
                    bufferedSink.close();
                }
            } finally {
                // The creation of the result must be done after the 'zipStream' is closed
                if (succeeded) {
                    // TODO further optimize by passing buffer and using okio http
                    result = new Transmission(buffer.readByteArray(), GZIP_WEB_CONTENT_TYPE, GZIP_WEB_ENCODING_TYPE);
                }
                buffer.clear();
            }
        } catch(Exception e) {
            logger.error("Failed to serialize , exception: {}", e.toString());
        }

        return Optional.fromNullable(result);
    }

    private boolean compress(BufferedSink sink, Collection<Telemetry> telemetries) throws IOException {
        int counter = 0;


        // The format is:
        // 1. Separate each Telemetry by newline
        // 2. Compress the entire data by using Gzip
        for (Telemetry telemetry : telemetries) {

            if (counter != 0) {
                sink.write(newlineString);
            }

            try {
                JsonTelemetryDataSerializer jsonWriter = new JsonTelemetryDataSerializer(JsonWriter.of(sink));
                telemetry.serialize(jsonWriter);
                jsonWriter.close();
                telemetry.markUsed();
                ++counter;
            } catch (IOException e) {
                logger.error("Failed to serialize Telemetry");
                logger.trace("Failed to serialize Telemetry", e);
            }
        }

        return counter > 0;
    }

    private boolean compressFromStrings(BufferedSink sink, Collection<String> telemetries) throws IOException {
        int counter = 0;

        // The format is:
        // 1. Separate each Telemetry by newline
        // 2. Compress the entire data by using Gzip
        for (String telemetry : telemetries) {

            if (counter != 0) {
                sink.write(newlineString);
            }

            try {
                sink.write(telemetry.getBytes());
                ++counter;
            } catch (Exception e) {
                logger.error("Failed to serialize , exception: {}", e.toString());
            }
        }

        return counter > 0;
    }
}
