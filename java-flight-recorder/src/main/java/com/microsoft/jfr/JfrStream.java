// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.jfr;

import java.io.IOException;
import java.io.InputStream;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * A JFR data stream backed by {@code FlightRecorderMXBean#readStream(long)}.
 */
class JfrStream extends InputStream {

    /* A default value for blockSize used by FlightRecorderMXBean#readStream(long) */
    private static final long DEFAULT_BLOCKSIZE = Long.getLong("jfr.stream.blocksize", 50000L);

    /**
     * Get the default value for blockSize used to configure the FlightRecorderMXBean#readStream(long) stream.
     * The default is configurable by setting the {@code jfrstream.blocksize} system property.
     * @return The default blockSize for reading flight recording data
     */
    public static long getDefaultBlockSize() { return DEFAULT_BLOCKSIZE; }

    private byte[] buffer;
    private int index = 0;
    private boolean EOF = false;
    // There is a recording id and an id you get from the recording for the stream.
    // streamId is the id for the stream.
    private final long streamid;
    private final MBeanServerConnection connection;
    private final ObjectName flightRecorder;

    JfrStream(MBeanServerConnection connection, ObjectName flightRecorder, long streamid) {
        this.streamid = streamid;
        this.connection = connection;
        this.flightRecorder = flightRecorder;
    }

    @Override
    public int read() throws IOException {

        if (!EOF && index == 0) {
            Object[] params = new Object[] {streamid};
            String[] signature = new String[] {long.class.getName()};
            try {
                buffer = (byte[]) connection.invoke(flightRecorder, "readStream", params, signature);
                EOF = buffer == null;
            } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
                throw new InternalError(e.getMessage(), e);
            }
        }

        if (EOF) return -1;

        int b = buffer[index] & 0xFF;
        index = ++index % buffer.length;
        return b;
    }

    @Override
    public void close() throws IOException {
        Object[] params = new Object[] {streamid};
        String[] signature = new String[] {long.class.getName()};
        try {
            connection.invoke(flightRecorder, "closeStream", params, signature);
        } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }
}
