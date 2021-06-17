package com.microsoft.applicationinsights.internal.persistence;

import com.google.common.io.Resources;
import com.squareup.moshi.JsonDataException;
import okio.BufferedSource;
import okio.Okio;
import org.junit.After;
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

import static com.microsoft.applicationinsights.internal.persistence.AppInsightsFileLoader.DEFAULT_FOlDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppInsightsFileWriterTests {

    @After
    public void cleanup() {
        Queue<String> queue = AppInsightsFileLoader.get().getPersistedFilesQueue();
        for (String filename : queue) {
            File tempFile = new File(DEFAULT_FOlDER, filename);
            assertTrue(tempFile.exists());
            assertTrue(tempFile.delete());
        }
    }

    @Test
    public void testWrite() throws IOException, JsonDataException {
        Path path = new File(Resources.getResource("bytebuffers.txt").getPath()).toPath();
        InputStream in = Files.newInputStream(path);
        BufferedSource source = Okio.buffer(Okio.source(in));
        String bytesToString = new String(source.readByteArray());

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

        AppInsightsFileWriter writer = new AppInsightsFileWriter();
        assertTrue(writer.writeToDisk(byteBuffers));
        assertEquals(1, AppInsightsFileLoader.get().getPersistedFilesQueue().size());
    }
}
