package com.microsoft.applicationinsights.internal.persistence;

import com.google.common.io.Resources;
import com.microsoft.applicationinsights.internal.authentication.AadAuthentication;
import com.squareup.moshi.JsonDataException;
import okio.BufferedSource;
import okio.Okio;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.DEFAULT_FOlDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalFileWriterTests {

    private byte[] rawBytes;

    @Before
    public void setup() {
        /**
         * AadAuthentication is used by TelemetryChannel, which is used to initialize {@link LocalFileLoader}
         */
        AadAuthentication.init(null, null, null, null, null, null);

        Path path = new File(Resources.getResource("write-transmission.txt").getPath()).toPath();
        try {
            InputStream in = Files.newInputStream(path);
            BufferedSource source = Okio.buffer(Okio.source(in));
            rawBytes = source.readByteArray();
        } catch (IOException ignore) {}
    }

    @After
    public void cleanup() {
        Queue<String> queue = LocalFileLoader.get().getPersistedFilesQueue();
        String filename = null;
        while((filename = queue.poll()) != null) {
            File tempFile = new File(DEFAULT_FOlDER, filename);
            assertTrue(tempFile.exists());
            assertTrue(tempFile.delete());
        }
    }

    @Test
    public void testWriteByteBuffersList() throws IOException, JsonDataException {
        String bytesToString = new String(rawBytes);

        List<ByteBuffer> byteBuffers = new ArrayList<>();
        String[] telemetries = bytesToString.split("\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < telemetries.length; i++) {
            baos.write(telemetries[i].getBytes());
            if (i < telemetries.length - 1) {
                baos.write('\r');
            }

            byteBuffers.add(ByteBuffer.wrap(baos.toByteArray()));
            baos.reset();
        }
        baos.close();

        assertEquals(10, byteBuffers.size());

        LocalFileWriter writer = new LocalFileWriter();
        assertTrue(writer.writeToDisk(byteBuffers));
        assertEquals(1, LocalFileLoader.get().getPersistedFilesQueue().size());
    }

    @Test
    public void testWriteRawByteArray() {
        LocalFileWriter writer = new LocalFileWriter();
        assertTrue(writer.writeToDisk(rawBytes));
        assertEquals(1, LocalFileLoader.get().getPersistedFilesQueue().size());
    }
}
