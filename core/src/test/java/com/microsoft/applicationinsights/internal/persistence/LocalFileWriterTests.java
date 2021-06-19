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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        Queue<String> queue = LocalFileLoader.get().getPersistedFilesCache();
        String filename = null;
        while((filename = queue.poll()) != null) {
            File tempFile = new File(PersistenceHelper.getDefaultSubdirectory(), filename);
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
        assertEquals(1, LocalFileLoader.get().getPersistedFilesCache().size());
    }

    @Test
    public void testWriteRawByteArray() {
        LocalFileWriter writer = new LocalFileWriter();
        assertTrue(writer.writeToDisk(rawBytes));
        assertEquals(1, LocalFileLoader.get().getPersistedFilesCache().size());
    }

    @Test
    public void testWriteUnderMultipleThreadsEnvironment() throws InterruptedException {
        String telemetry = "{\"ver\":1,\"name\":\"Metric\",\"time\":\"2021-06-14T17:24:28.983-0700\",\"sampleRate\":100,\"iKey\":\"00000000-0000-0000-0000-0FEEDDADBEEF\",\"tags\":{\"ai.internal.sdkVersion\":\"java:3.1.1\",\"ai.internal.nodeName\":\"test-role-name\",\"ai.cloud.roleInstance\":\"test-role-instance\"},\"data\":{\"baseType\":\"MetricData\",\"baseData\":{\"ver\":2,\"metrics\":[{\"name\":\"jvm_threads_states\",\"kind\":0,\"value\":3}],\"properties\":{\"state\":\"blocked\"}}}}";

        System.out.println(telemetry.getBytes().length);

        final ExecutorService executorService = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 100; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        LocalFileWriter writer = new LocalFileWriter();
                        writer.writeToDisk(telemetry.getBytes());
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        assertEquals(1000, LocalFileLoader.get().getPersistedFilesCache().size());
    }
}
