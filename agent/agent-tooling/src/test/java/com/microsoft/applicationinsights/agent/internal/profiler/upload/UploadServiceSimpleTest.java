// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.test.http.NoOpHttpClient;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// TODO (trask) these tests do not make any http calls, is there a better way to write them that
class UploadServiceSimpleTest {

  @Test
  void roleNameIsCorrectlyAddedToMetaData() throws IOException {

    HttpPipeline httpPipeline = new HttpPipelineBuilder().httpClient(new NoOpHttpClient()).build();
    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            new URL("https://agent.azureserviceprofiler.net/"),
            "00000000-0000-0000-0000-000000000000",
            httpPipeline);

    File tmpFile = createFakeJfrFile();
    UUID appId = UUID.randomUUID();

    BlobUploadFromFileOptions blobOptions =
        new UploadService(
                serviceProfilerClient,
                builder -> {},
                "a-machine-name",
                "a-process-id",
                appId::toString,
                "a-role-name")
            .createBlockBlobOptions(
                tmpFile,
                UploadContext.builder()
                    .setMachineName("a-machine-name")
                    .setDataCube(UUID.randomUUID())
                    .setSessionId(1)
                    .setTraceFile(tmpFile)
                    .setProfileId(UUID.randomUUID())
                    .setFileFormat("jfr")
                    .setExtension("jfr")
                    .build());

    // Role name is set correctly
    assertThat(blobOptions.getMetadata().get(UploadService.ROLE_NAME_META_NAME))
        .isEqualTo("a-role-name");

    blobOptions =
        new UploadService(
                serviceProfilerClient,
                builder -> {},
                "a-machine-name",
                "a-process-id",
                appId::toString,
                null)
            .createBlockBlobOptions(
                tmpFile,
                UploadContext.builder()
                    .setMachineName("a-machine-name")
                    .setDataCube(UUID.randomUUID())
                    .setSessionId(1)
                    .setTraceFile(tmpFile)
                    .setProfileId(UUID.randomUUID())
                    .setFileFormat("jfr")
                    .setExtension("jfr")
                    .build());

    // Null role name tag is not added
    assertThat(blobOptions.getMetadata().get(UploadService.ROLE_NAME_META_NAME)).isNull();
  }

  @Test
  void uploadWithoutFileThrows() throws MalformedURLException {

    HttpPipeline httpPipeline = new HttpPipelineBuilder().httpClient(new NoOpHttpClient()).build();
    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            new URL("https://agent.azureserviceprofiler.net/"),
            "00000000-0000-0000-0000-000000000000",
            httpPipeline);

    UUID appId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    UploadService uploadService =
        new UploadService(
            serviceProfilerClient,
            builder -> {},
            "a-machine-name",
            "a-process-id",
            appId::toString,
            "a-role-name");

    assertThatThrownBy(
            () ->
                uploadService
                    .uploadJfrFile(profileId, "a-trigger", 321, new File("./not-a-file"), 0.0, 0.0)
                    .block())
        .hasRootCauseInstanceOf(NoSuchFileException.class);
  }

  private static File createFakeJfrFile() throws IOException {
    File tmpFile = File.createTempFile("a-jfr-file", "jfr");
    FileOutputStream fos = new FileOutputStream(tmpFile);
    fos.write("foobar".getBytes(UTF_8));
    fos.close();
    tmpFile.deleteOnExit();
    return tmpFile;
  }
}
