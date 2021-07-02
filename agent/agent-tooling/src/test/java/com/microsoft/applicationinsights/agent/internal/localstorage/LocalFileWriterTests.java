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

package com.microsoft.applicationinsights.agent.internal.localstorage;

import static com.microsoft.applicationinsights.agent.internal.localstorage.PersistenceHelper.DEFAULT_FOLDER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

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
import okio.BufferedSource;
import okio.Okio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocalFileWriterTests {

  private LocalFileCache localFileCache;

  private ByteBuffer buffer;

  @BeforeEach
  public void setup() {
    localFileCache = new LocalFileCache();

    Path path =
        new File(getClass().getClassLoader().getResource("write-transmission.txt").getPath())
            .toPath();
    try {
      InputStream in = Files.newInputStream(path);
      BufferedSource source = Okio.buffer(Okio.source(in));
      buffer = ByteBuffer.wrap(source.readByteArray());
    } catch (IOException ignored) {
    }
  }

  @AfterEach
  public void cleanup() {
    Queue<String> queue = localFileCache.getPersistedFilesCache();
    String filename;
    File defaultFolder = PersistenceHelper.getDefaultFolder(false);
    while ((filename = queue.poll()) != null) {
      File tempFile = new File(defaultFolder, filename);
      assertThat(tempFile.exists()).isTrue();
      assertThat(tempFile.delete()).isTrue();
    }

    if (defaultFolder.exists()) {
      assertThat(defaultFolder.delete()).isTrue();
    }

    if (DEFAULT_FOLDER.exists()) {
      assertThat(DEFAULT_FOLDER.delete()).isTrue();
    }
  }

  @Test
  public void testWriteByteBuffersList() throws IOException {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    String bytesToString = new String(bytes, UTF_8);

    List<ByteBuffer> byteBuffers = new ArrayList<>();
    String[] telemetries = bytesToString.split("\n");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (int i = 0; i < telemetries.length; i++) {
      baos.write(telemetries[i].getBytes(UTF_8));
      if (i < telemetries.length - 1) {
        baos.write('\r');
      }

      byteBuffers.add(ByteBuffer.wrap(baos.toByteArray()));
      baos.reset();
    }
    baos.close();

    assertThat(byteBuffers.size()).isEqualTo(10);

    LocalFileWriter writer = new LocalFileWriter(localFileCache, false);
    assertThat(writer.writeToDisk(byteBuffers)).isTrue();
    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(1);
  }

  @Test
  public void testWriteRawByteArray() {
    LocalFileWriter writer = new LocalFileWriter(localFileCache, false);
    assertThat(writer.writeToDisk(singletonList(buffer))).isTrue();
    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(1);
  }

  @Test
  public void testWriteUnderMultipleThreadsEnvironment() throws InterruptedException {
    String telemetry =
        "{\"ver\":1,\"name\":\"Metric\",\"time\":\"2021-06-14T17:24:28.983-0700\",\"sampleRate\":100,\"iKey\":\"00000000-0000-0000-0000-0FEEDDADBEEF\",\"tags\":{\"ai.internal.sdkVersion\":\"java:3.1.1\",\"ai.internal.nodeName\":\"test-role-name\",\"ai.cloud.roleInstance\":\"test-role-instance\"},\"data\":{\"baseType\":\"MetricData\",\"baseData\":{\"ver\":2,\"metrics\":[{\"name\":\"jvm_threads_states\",\"kind\":0,\"value\":3}],\"properties\":{\"state\":\"blocked\"}}}}";

    ExecutorService executorService = Executors.newFixedThreadPool(100);
    for (int i = 0; i < 100; i++) {
      executorService.execute(
          () -> {
            for (int j = 0; j < 10; j++) {
              LocalFileWriter writer = new LocalFileWriter(localFileCache, false);
              writer.writeToDisk(singletonList(ByteBuffer.wrap(telemetry.getBytes(UTF_8))));
            }
          });
    }

    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.MINUTES);
    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(1000);
  }
}
