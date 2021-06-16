package com.microsoft.applicationinsights.internal.persistence;

import com.google.common.io.Resources;
import com.squareup.moshi.JsonDataException;
import okio.BufferedSource;
import okio.Okio;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.microsoft.applicationinsights.internal.persistence.FileLoader.DEFAULT_FOlDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileWriterTests {

    @After
    public void cleanup() {
        Queue<String> queue = FileLoader.get().getPersistedFilesQueue();
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
        List<ByteBuffer> byteBuffers = new ArrayList<>();
        while(true) {
            String line = source.readUtf8Line();
            if (line == null) {
                break;
            }

            byteBuffers.add(ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8)));
        }

        FileWriter writer = new FileWriter();
        assertEquals(10, byteBuffers.size());
        assertTrue(writer.write(byteBuffers));
        assertEquals(1, FileLoader.get().getPersistedFilesQueue().size());
    }
}
