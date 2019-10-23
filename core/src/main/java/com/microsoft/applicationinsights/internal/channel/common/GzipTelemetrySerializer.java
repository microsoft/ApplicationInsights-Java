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
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import com.google.common.base.Charsets;
import com.microsoft.applicationinsights.internal.channel.TelemetrySerializer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The class is an implementation of the {@link TelemetrySerializer}
 * where the {@link Telemetry} instances are compressed by Gzip after converted to Json format
 *
 * Created by gupele on 12/17/2014.
 */
public final class GzipTelemetrySerializer implements TelemetrySerializer {
    private final static String GZIP_WEB_CONTENT_TYPE = "application/x-json-stream";
    private final static String GZIP_WEB_ENCODING_TYPE = "gzip";

    private final ThreadLocal<ByteArrayOutputStream> byteStreams =
            new ThreadLocal<ByteArrayOutputStream>() {
                @Override
                protected ByteArrayOutputStream initialValue() {
                    return new ByteArrayOutputStream(1024 * 8);
                }
            };

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
            ByteArrayOutputStream byteStream = byteStreams.get();
            byteStream.reset();

            try {
                GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);

                try {
                    succeeded = compress(zipStream, telemetries);
                } catch (Exception e) {
                    InternalLogger.INSTANCE.error("Failed to serialize , exception: %s", e.toString());
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    try {
                        InternalLogger.INSTANCE.error("Failed to serialize, unknown exception: %s", t.toString());                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t2) {
                        // chomp
                    }
                } finally {
                    zipStream.close();
                }
            } finally {
                byteStream.close();

                // The creation of the result must be done after the 'zipStream' is closed
                if (succeeded) {
                    result = new Transmission(byteStream.toByteArray(), GZIP_WEB_CONTENT_TYPE, GZIP_WEB_ENCODING_TYPE);
                    if (byteStream.size() > 1024 * 1024) {
                        // in case it gets unexpectedly large
                        byteStreams.remove();
                    }
                }
            }
        } catch(Exception e) {
            InternalLogger.INSTANCE.error("Failed to serialize , exception: %s", e.toString());
        }

        return Optional.fromNullable(result);
    }

    public Optional<Transmission> serializeFromStrings(Collection<String> telemetries) {
        Preconditions.checkNotNull(telemetries, "telemetries must be non-null value");
        Preconditions.checkArgument(!telemetries.isEmpty(), "telemetries: One or more telemetry item is expected");

        Transmission result = null;
        boolean succeeded = false;
        try {
            ByteArrayOutputStream byteStream = byteStreams.get();
            byteStream.reset();

            try {
                GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);

                try {
                    succeeded = compressFromStrings(zipStream, telemetries);
                } catch (Exception e) {
                    InternalLogger.INSTANCE.error("Failed to serialize , exception: %s", e.toString());
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    try {
                        InternalLogger.INSTANCE.error("Failed to serialize, unknown exception: %s", t.toString());                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t2) {
                        // chomp
                    }
                } finally {
                    zipStream.close();
                }
            } finally {
                byteStream.close();

                // The creation of the result must be done after the 'zipStream' is closed
                if (succeeded) {
                    result = new Transmission(byteStream.toByteArray(), GZIP_WEB_CONTENT_TYPE, GZIP_WEB_ENCODING_TYPE);
                    if (byteStream.size() > 1024 * 1024) {
                        // in case it gets unexpectedly large
                        byteStreams.remove();
                    }
                }
            }
        } catch(Exception e) {
            InternalLogger.INSTANCE.error("Failed to serialize , exception: %s", e.toString());
        }

        return Optional.fromNullable(result);
    }

    private boolean compress(GZIPOutputStream zipStream, Collection<Telemetry> telemetries) throws IOException {
        int counter = 0;

        // The format is:
        // 1. Separate each Telemetry by newline
        // 2. Compress the entire data by using Gzip
        for (Telemetry telemetry : telemetries) {

            if (counter != 0) {
                zipStream.write(newlineString);
            }

            try {
                JsonTelemetryDataSerializer jsonWriter =
                        new JsonTelemetryDataSerializer(new OutputStreamWriter(zipStream, Charsets.UTF_8));
                telemetry.serialize(jsonWriter);
                jsonWriter.close();
                telemetry.markUsed();
                ++counter;
            } catch (IOException e) {
                InternalLogger.INSTANCE.error("Failed to serialize Telemetry");
                InternalLogger.INSTANCE.trace("Stack trace is %s", ExceptionUtils.getStackTrace(e));
            }
        }

        return counter > 0;
    }

    private boolean compressFromStrings(GZIPOutputStream zipStream, Collection<String> telemetries) throws IOException {
        int counter = 0;

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
                InternalLogger.INSTANCE.error("Failed to serialize , exception: %s", e.toString());
            }
        }

        return counter > 0;
    }
}
