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

package com.azure.monitor.opentelemetry.exporter.implementation.localstorage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.Context;
import com.azure.monitor.opentelemetry.exporter.implementation.MockHttpResponse;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipeline;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;

public class LocalFileLoaderTests {

  private static final String GZIPPED_RAW_BYTES_WITHOUT_IKEY = "gzipped-raw-bytes-without-ikey.trn";
  private static final String GZIPPED_RAW_BYTES_WITHOUT_INGESTION_ENDPOINT =
      "gzipped-raw-bytes-without-ingestion-endpoint.trn";
  private static final String INSTRUMENTATION_KEY = "00000000-0000-0000-0000-0FEEDDADBEEF";
  private static final String INGESTION_ENDPOINT = "http://foo.bar/";

  @TempDir File tempFolder;

  @Test
  public void testInstrumentationKeyRegex() {
    assertThat(LocalFileLoader.isInstrumentationKeyValid(INSTRUMENTATION_KEY)).isTrue();
    assertThat(LocalFileLoader.isInstrumentationKeyValid("fake-instrumentation-key")).isFalse();
    assertThat(LocalFileLoader.isInstrumentationKeyValid("5ED1AE38-41AF-11EC-81D3")).isFalse();
    assertThat(LocalFileLoader.isInstrumentationKeyValid("5ED1AE38-41AF-11EC-81D3-0242AC130003"))
        .isTrue();
    assertThat(LocalFileLoader.isInstrumentationKeyValid("C6864988-6BF8-45EF-8590-1FD3D84E5A4D"))
        .isTrue();
  }

  @Test
  public void testPersistedFileWithoutInstrumentationKey() throws IOException {
    File persistedFile = new File(tempFolder, GZIPPED_RAW_BYTES_WITHOUT_IKEY);
    byte[] bytes = Resources.readBytes(GZIPPED_RAW_BYTES_WITHOUT_IKEY);
    Files.write(persistedFile.toPath(), bytes);
    assertThat(persistedFile.exists()).isTrue();

    LocalFileCache localFileCache = new LocalFileCache(tempFolder);
    localFileCache.addPersistedFile(persistedFile);

    LocalFileLoader localFileLoader = new LocalFileLoader(localFileCache, tempFolder, null, false);
    LocalFileLoader.PersistedFile loadedPersistedFile = localFileLoader.loadTelemetriesFromDisk();
    assertThat(loadedPersistedFile).isNull();
    assertThat(persistedFile.exists())
        .isFalse(); // verify the old formatted trn is deleted successfully.
  }

  @Test
  public void testPersistedFileWithoutIngestionEndpoint() throws IOException {

    File persistedFile = new File(tempFolder, GZIPPED_RAW_BYTES_WITHOUT_INGESTION_ENDPOINT);
    byte[] bytes = Resources.readBytes(GZIPPED_RAW_BYTES_WITHOUT_INGESTION_ENDPOINT);
    Files.write(persistedFile.toPath(), bytes);
    assertThat(persistedFile.exists()).isTrue();

    LocalFileCache localFileCache = new LocalFileCache(tempFolder);
    localFileCache.addPersistedFile(persistedFile);

    LocalFileLoader localFileLoader = new LocalFileLoader(localFileCache, tempFolder, null, false);
    LocalFileLoader.PersistedFile loadedPersistedFile = localFileLoader.loadTelemetriesFromDisk();
    assertThat(loadedPersistedFile).isNull();
    assertThat(persistedFile.exists())
        .isFalse(); // verify the old formatted trn is deleted successfully.
  }

  @Test
  public void testDeleteFilePermanentlyOnSuccess() throws Exception {
    HttpClient mockedClient = getMockHttpClientSuccess();
    HttpPipelineBuilder pipelineBuilder = new HttpPipelineBuilder().httpClient(mockedClient);
    LocalFileCache localFileCache = new LocalFileCache(tempFolder);
    LocalFileWriter localFileWriter =
        new LocalFileWriter(50, localFileCache, tempFolder, null, false);
    LocalFileLoader localFileLoader = new LocalFileLoader(localFileCache, tempFolder, null, false);

    TelemetryPipeline telemetryPipeline = new TelemetryPipeline(pipelineBuilder.build());

    // persist 10 files to disk
    for (int i = 0; i < 10; i++) {
      localFileWriter.writeToDisk(
          INSTRUMENTATION_KEY,
          INGESTION_ENDPOINT,
          singletonList(ByteBuffer.wrap("hello world".getBytes(UTF_8))));
    }

    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(10);

    List<File> files = FileUtil.listTrnFiles(tempFolder);
    assertThat(files.size()).isEqualTo(10);

    int expectedCount = 10;

    // send persisted files one by one and then delete it permanently.
    for (int i = 0; i < 10; i++) {
      LocalFileLoader.PersistedFile persistedFile = localFileLoader.loadTelemetriesFromDisk();
      CompletableResultCode completableResultCode =
          telemetryPipeline.send(
              singletonList(persistedFile.rawBytes),
              persistedFile.instrumentationKey,
              persistedFile.ingestionEndpoint,
              new LocalFileSenderTelemetryPipelineListener(localFileLoader, persistedFile.file));
      completableResultCode.join(10, SECONDS);
      assertThat(completableResultCode.isSuccess()).isEqualTo(true);

      // sleep 1 second to wait for delete to complete
      Thread.sleep(1000);

      files = FileUtil.listTrnFiles(tempFolder);
      assertThat(files.size()).isEqualTo(--expectedCount);
    }

    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(0);
  }

  @Test
  public void testDeleteFilePermanentlyOnFailure() throws Exception {
    HttpClient mockedClient = mock(HttpClient.class);
    when(mockedClient.send(any(HttpRequest.class), any(Context.class)))
        .then(
            invocation ->
                Mono.error(
                    () -> new Exception("this is expected to be logged by the operation logger")));
    HttpPipelineBuilder pipelineBuilder = new HttpPipelineBuilder().httpClient(mockedClient);
    LocalFileCache localFileCache = new LocalFileCache(tempFolder);

    LocalFileLoader localFileLoader = new LocalFileLoader(localFileCache, tempFolder, null, false);
    LocalFileWriter localFileWriter =
        new LocalFileWriter(50, localFileCache, tempFolder, null, false);

    TelemetryPipeline telemetryPipeline = new TelemetryPipeline(pipelineBuilder.build());

    // persist 10 files to disk
    for (int i = 0; i < 10; i++) {
      localFileWriter.writeToDisk(
          INSTRUMENTATION_KEY,
          INGESTION_ENDPOINT,
          singletonList(ByteBuffer.wrap("hello world".getBytes(UTF_8))));
    }

    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(10);

    List<File> files = FileUtil.listTrnFiles(tempFolder);
    assertThat(files.size()).isEqualTo(10);

    // fail to send persisted files and expect them to be kept on disk
    for (int i = 0; i < 10; i++) {
      LocalFileLoader.PersistedFile persistedFile = localFileLoader.loadTelemetriesFromDisk();
      assertThat(persistedFile.instrumentationKey).isEqualTo(INSTRUMENTATION_KEY);

      CompletableResultCode completableResultCode =
          telemetryPipeline.send(
              singletonList(persistedFile.rawBytes),
              persistedFile.instrumentationKey,
              persistedFile.ingestionEndpoint,
              new LocalFileSenderTelemetryPipelineListener(localFileLoader, persistedFile.file));
      completableResultCode.join(10, SECONDS);
      assertThat(completableResultCode.isSuccess()).isEqualTo(false);
    }

    files = FileUtil.listTrnFiles(tempFolder);
    assertThat(files.size()).isEqualTo(10);
    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(10);
  }

  private static HttpClient getMockHttpClientSuccess() {
    return new MockHttpClient(
        request -> {
          return Mono.just(new MockHttpResponse(request, 200));
        });
  }

  private static class MockHttpClient implements HttpClient {
    private final Function<HttpRequest, Mono<HttpResponse>> handler;

    MockHttpClient(Function<HttpRequest, Mono<HttpResponse>> handler) {
      this.handler = handler;
    }

    @Override
    public Mono<HttpResponse> send(HttpRequest httpRequest) {
      return handler.apply(httpRequest);
    }
  }
}
