// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.microsoft.applicationinsights.agent.internal.profiler.service.BlobAccessPass;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class UploadServiceTest {
  @Test
  void uploadFileGoodPathReturnsExpectedResponse() throws IOException {

    ServiceProfilerClient serviceProfilerClient = stubServiceProfilerClient();

    File tmpFile = createFakeJfrFile();
    UUID appId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    UploadService uploadService =
        new UploadService(
            serviceProfilerClient,
            "a-machine-name",
            "a-process-id",
            appId::toString,
            "a-role-name") {
          @Override
          protected Mono<UploadFinishArgs> performUpload(
              UploadContext uploadContext, BlobAccessPass uploadPass, File file) {
            return Mono.just(new UploadFinishArgs("a-stamp-id", "a-timestamp"));
          }
        };

    uploadService
        .uploadJfrFile(profileId, "a-trigger", 321, tmpFile, 0.0, 0.0)
        .subscribe(
            result -> {
              assertThat(
                      result
                          .getServiceProfilerIndex()
                          .getProperties()
                          .get(ServiceProfilerIndex.Builder.SERVICE_PROFILER_STAMPID_PROPERTY_NAME))
                  .isEqualTo("a-stamp-id");

              assertThat(
                      result
                          .getServiceProfilerIndex()
                          .getProperties()
                          .get(
                              ServiceProfilerIndex.Builder
                                  .SERVICE_PROFILER_MACHINENAME_PROPERTY_NAME))
                  .isEqualTo("a-machine-name");

              assertThat(
                      result
                          .getServiceProfilerIndex()
                          .getProperties()
                          .get(
                              ServiceProfilerIndex.Builder
                                  .SERVICE_PROFILER_ETLFILESESSIONID_PROPERTY_NAME))
                  .isEqualTo("a-timestamp");

              assertThat(
                      result
                          .getServiceProfilerIndex()
                          .getProperties()
                          .get(
                              ServiceProfilerIndex.Builder.SERVICE_PROFILER_DATACUBE_PROPERTY_NAME))
                  .isEqualTo(appId.toString());
            });
  }

  @Test
  void roleNameIsCorrectlyAddedToMetaData() throws IOException {

    ServiceProfilerClient serviceProfilerClient = stubServiceProfilerClient();

    File tmpFile = createFakeJfrFile();
    UUID appId = UUID.randomUUID();

    BlobUploadFromFileOptions blobOptions =
        new UploadService(
                serviceProfilerClient,
                "a-machine-name",
                "a-process-id",
                appId::toString,
                "a-role-name")
            .createBlockBlobOptions(
                tmpFile,
                new UploadContext(
                    "a-machine-name",
                    UUID.randomUUID(),
                    1,
                    tmpFile,
                    UUID.randomUUID(),
                    "jfr",
                    "jfr"));

    // Role name is set correctly
    assertThat(blobOptions.getMetadata().get(UploadService.ROLE_NAME_META_NAME))
        .isEqualTo("a-role-name");

    blobOptions =
        new UploadService(
                serviceProfilerClient, "a-machine-name", "a-process-id", appId::toString, null)
            .createBlockBlobOptions(
                tmpFile,
                new UploadContext(
                    "a-machine-name",
                    UUID.randomUUID(),
                    1,
                    tmpFile,
                    UUID.randomUUID(),
                    "jfr",
                    "jfr"));

    // Null role name tag is not added
    assertThat(blobOptions.getMetadata().get(UploadService.ROLE_NAME_META_NAME)).isNull();
  }

  @Test
  void uploadWithoutFileThrows() {

    ServiceProfilerClient serviceProfilerClient = stubServiceProfilerClient();

    UUID appId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    UploadService uploadService =
        new UploadService(
            serviceProfilerClient,
            "a-machine-name",
            "a-process-id",
            appId::toString,
            "a-role-name");

    AtomicBoolean threw = new AtomicBoolean(false);
    uploadService
        .uploadJfrFile(profileId, "a-trigger", 321, new File("not-a-file"), 0.0, 0.0)
        .subscribe(result -> {}, e -> threw.set(true));

    assertThat(threw.get()).isTrue();
  }

  private static File createFakeJfrFile() throws IOException {
    File tmpFile = File.createTempFile("a-jfr-file", "jfr");
    FileOutputStream fos = new FileOutputStream(tmpFile);
    fos.write("foobar".getBytes(UTF_8));
    fos.close();
    tmpFile.deleteOnExit();
    return tmpFile;
  }

  private static ServiceProfilerClient stubServiceProfilerClient() {
    ServiceProfilerClient mock = mock(ServiceProfilerClient.class);
    when(mock.getUploadAccess(any(UUID.class), any(String.class)))
        .thenReturn(
            Mono.just(
                new BlobAccessPass("https://localhost:99999/a-blob-uri", null, "a-sas-token")));

    return mock;
  }
}
