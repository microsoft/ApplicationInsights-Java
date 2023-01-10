// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.test.TestBase;
import com.azure.core.test.TestMode;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UploadServiceTest extends TestBase {

  @Test
  void uploadFileGoodPathReturnsExpectedResponse() throws IOException {

    HttpPipeline httpPipeline = getHttpPipeline();
    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            new URL("https://agent.azureserviceprofiler.net/"),
            "00000000-0000-0000-0000-000000000000",
            httpPipeline);

    File tmpFile = createFakeJfrFile();
    UUID appId = UUID.randomUUID();
    UUID profileId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    UploadService uploadService =
        new UploadService(
            serviceProfilerClient,
            blobContainerClientBuilder -> customize(blobContainerClientBuilder),
            "a-machine-name",
            "a-process-id",
            appId::toString,
            "a-role-name");

    ServiceProfilerIndex serviceProfilerIndex =
        uploadService.uploadJfrFile(profileId, "a-trigger", 321, tmpFile, 0.0, 0.0).block();

    assertThat(
            serviceProfilerIndex
                .getProperties()
                .get(ServiceProfilerIndex.Builder.SERVICE_PROFILER_STAMPID_PROPERTY_NAME))
        .isEqualTo("westus2-ey2ahqc2dsyvq");

    assertThat(
            serviceProfilerIndex
                .getProperties()
                .get(ServiceProfilerIndex.Builder.SERVICE_PROFILER_MACHINENAME_PROPERTY_NAME))
        .isEqualTo("a-machine-name");

    assertThat(
            serviceProfilerIndex
                .getProperties()
                .get(ServiceProfilerIndex.Builder.SERVICE_PROFILER_ETLFILESESSIONID_PROPERTY_NAME))
        .isEqualTo("2022-10-31T02:35:34.0337660Z");

    assertThat(
            serviceProfilerIndex
                .getProperties()
                .get(ServiceProfilerIndex.Builder.SERVICE_PROFILER_DATACUBE_PROPERTY_NAME))
        .isEqualTo(appId.toString());
  }

  private static File createFakeJfrFile() throws IOException {
    File tmpFile = File.createTempFile("a-jfr-file", "jfr");
    FileOutputStream fos = new FileOutputStream(tmpFile);
    fos.write("foobar".getBytes(UTF_8));
    fos.close();
    tmpFile.deleteOnExit();
    return tmpFile;
  }

  private HttpPipeline getHttpPipeline() {
    if (getTestMode() == TestMode.RECORD || getTestMode() == TestMode.LIVE) {
      return new HttpPipelineBuilder()
          .httpClient(HttpClient.createDefault())
          .policies(interceptorManager.getRecordPolicy())
          .build();
    } else {
      return new HttpPipelineBuilder().httpClient(interceptorManager.getPlaybackClient()).build();
    }
  }

  private void customize(BlobContainerClientBuilder blobContainerClientBuilder) {
    if (getTestMode() == TestMode.RECORD || getTestMode() == TestMode.LIVE) {
      blobContainerClientBuilder.addPolicy(interceptorManager.getRecordPolicy());
    } else {
      blobContainerClientBuilder.httpClient(interceptorManager.getPlaybackClient());
    }
  }
}
