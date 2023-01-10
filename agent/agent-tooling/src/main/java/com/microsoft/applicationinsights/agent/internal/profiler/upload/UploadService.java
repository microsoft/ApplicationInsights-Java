// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.microsoft.applicationinsights.agent.internal.profiler.service.BlobAccessPass;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import com.microsoft.applicationinsights.agent.internal.profiler.util.OsPlatformProvider;
import com.microsoft.applicationinsights.agent.internal.profiler.util.TimestampContract;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Uploads profiles to the service profiler endpoint. */
public class UploadService {

  private static final String DATA_CUBE_META_NAME = "spDataCube";
  private static final String MACHINE_NAME_META_NAME = "spMachineName";
  private static final String START_TIME_META_NAME = "spTraceStartTime";
  private static final String PROGRAMMING_LANGUAGE_META_NAME = "spProgrammingLanguage";
  private static final String OS_PLATFORM_META_NAME = "spOSPlatform";
  private static final String TRACE_FILE_FORMAT_META_NAME = "spTraceFileFormat";
  // Visible for testing
  static final String ROLE_NAME_META_NAME = "RoleName";

  private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

  private static final long UPLOAD_BLOCK_LENGTH = 8 * 1024 * 1024;

  // For debug purposes, can use settings to tell the profiler to retain the profile after
  // it has been uploaded
  private static final String RETAIN_JFR_FILE_PROPERTY_NAME =
      "applicationinsights.debug.retainJfrFile";
  private static final boolean retainJfrFile =
      Boolean.parseBoolean(System.getProperty(RETAIN_JFR_FILE_PROPERTY_NAME, "false"));

  private final ServiceProfilerClient serviceProfilerClient;
  // only used by tests
  private final Consumer<BlobContainerClientBuilder> blobContainerClientCustomizer;
  private final String machineName;
  private final Supplier<String> appIdSupplier;
  private final String processId;
  private final String roleName;

  public UploadService(
      ServiceProfilerClient serviceProfilerClient,
      Consumer<BlobContainerClientBuilder> blobContainerClientCustomizer,
      String machineName,
      String processId,
      Supplier<String> appIdSupplier,
      String roleName) {
    this.serviceProfilerClient = serviceProfilerClient;
    this.blobContainerClientCustomizer = blobContainerClientCustomizer;
    this.machineName = machineName;
    this.processId = processId;
    this.appIdSupplier = appIdSupplier;
    this.roleName = roleName;
  }

  public void upload(
      AlertBreach alertBreach, long timestamp, File file, UploadListener uploadListener) {

    String appId = appIdSupplier.get();
    if (appId == null || appId.isEmpty()) {
      logger.error("Not uploading file due to lack of app id");
      return;
    }

    uploadJfrFile(
            UUID.fromString(alertBreach.getProfileId()),
            "JFR-" + alertBreach.getType().name(),
            timestamp,
            file,
            alertBreach.getCpuMetric(),
            alertBreach.getMemoryUsage())
        .subscribe(onUploadComplete(uploadListener), e -> logger.error("Failed to upload file", e));
  }

  // Notify listener that full profile and upload cycle has completed and log success
  private static Consumer<? super ServiceProfilerIndex> onUploadComplete(
      UploadListener uploadListener) {
    return result -> {
      uploadListener.onUpload(result);
      logger.info("Uploading of profile complete");
    };
  }

  /** Upload a given JFR file and return associated metadata of the uploaded profile. */
  // visible for tests
  Mono<ServiceProfilerIndex> uploadJfrFile(
      UUID profileId,
      String triggerName,
      long timestamp,
      File file,
      double cpuUsage,
      double memoryUsage) {

    return uploadFile(
        triggerName, timestamp, profileId, file, cpuUsage, memoryUsage, "Profile", "jfr", "jfr");
  }

  public Mono<ServiceProfilerIndex> uploadFile(
      String triggerName,
      long timestamp,
      UUID profileId,
      File file,
      double cpuUsage,
      double memoryUsage,
      String artifactKind,
      String extension,
      String fileFormat) {
    String appId = appIdSupplier.get();
    if (appId == null || appId.isEmpty()) {
      logger.error("Failed to upload due to lack of appId");
      return Mono.error(new UploadFailedException("Failed to upload due to lack of appId"));
    }

    UploadContext uploadContext =
        UploadContext.builder()
            .setMachineName(machineName)
            .setDataCube(UUID.fromString(appId))
            .setSessionId(timestamp)
            .setTraceFile(file)
            .setProfileId(profileId)
            .setFileFormat(fileFormat)
            .setExtension(extension)
            .build();

    // upload trace to service profiler
    return uploadTrace(uploadContext)
        .map(
            done -> {
              // return associated profile metadata
              String fileId = createId();
              String formattedTimestamp = TimestampContract.padNanos(done.getTimeStamp());

              return ServiceProfilerIndex.builder()
                  .setTriggeredBy(triggerName)
                  .setFileId(fileId)
                  .setStampId(done.getStampId())
                  .setDataCubeId(UUID.fromString(appId))
                  .setTimeStamp(formattedTimestamp)
                  .setMachineName(uploadContext.getMachineName())
                  .setOs(OsPlatformProvider.getOsPlatformDescription())
                  .setProcessId(processId)
                  .setArtifactKind(artifactKind)
                  .setArtifactId(profileId.toString())
                  .setExtension(extension)
                  .setCpuUsage(cpuUsage)
                  .setMemoryUsage(memoryUsage)
                  .build();
            });
  }

  @SuppressFBWarnings(
      value = "SECPR", // Predictable pseudorandom number generator
      justification = "Predictable random is ok for file id")
  private static String createId() {
    byte[] bytes = new byte[9];
    ThreadLocalRandom.current().nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  /** Upload profile to service profiler. */
  private Mono<UploadFinishArgs> uploadTrace(UploadContext uploadContext) {

    File zippedTraceFile = null;

    try {
      // Zip up profile
      zippedTraceFile = createZippedTraceFile(uploadContext);

      // Obtain permission to upload profile

      File finalZippedTraceFile1 = zippedTraceFile;
      return serviceProfilerClient
          .getUploadAccess(uploadContext.getProfileId(), uploadContext.getExtension())
          .flatMap(
              uploadPass -> {
                if (uploadPass == null) {
                  close(finalZippedTraceFile1);
                  return Mono.error(new UploadFailedException("Failed to obtain upload pass"));
                }

                // perform upload then finally close file
                return performUpload(uploadContext, uploadPass, finalZippedTraceFile1)
                    .doFinally((signal) -> close(finalZippedTraceFile1));
              });
    } catch (Exception e) {
      logger.error("Upload of the trace file failed", e);
      if (zippedTraceFile != null) {
        close(zippedTraceFile);
      }
      return Mono.error(new UploadFailedException(e));
    }
  }

  protected Mono<UploadFinishArgs> performUpload(
      UploadContext uploadContext, BlobAccessPass uploadPass, File file) {
    return uploadToSasLink(uploadPass, uploadContext, file)
        .flatMap(response -> reportUploadComplete(uploadContext, response));
  }

  /** Upload the given file to a blob storage defined by a sas link. */
  private Mono<Response<BlockBlobItem>> uploadToSasLink(
      BlobAccessPass uploadPass, UploadContext uploadContext, File file) {
    try {
      URL sasUrl = new URL(uploadPass.getUriWithSasToken());

      BlobUploadFromFileOptions options = createBlockBlobOptions(file, uploadContext);
      // TODO (trask) should we be injecting our HttpClient into the blob container client?
      BlobContainerClientBuilder builder =
          new BlobContainerClientBuilder().endpoint(sasUrl.toString());
      blobContainerClientCustomizer.accept(builder);
      BlobContainerAsyncClient blobContainerClient = builder.buildAsyncClient();

      BlobAsyncClient blobClient = blobContainerClient.getBlobAsyncClient(uploadPass.getBlobName());
      return blobClient
          .uploadFromFileWithResponse(options)
          .doFinally((done) -> logger.info("upload done"));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Malformed url", e);
    }
  }

  private static void close(File zippedTraceFile) {
    try {
      deletePathRecursive(zippedTraceFile);
    } catch (Exception e) {
      logger.warn("An error occurred when closing the zipped trace file", e);
    }
  }

  /** Report the success of an upload or throw an exception. */
  protected Mono<UploadFinishArgs> reportUploadComplete(
      UploadContext uploadContext, Response<BlockBlobItem> response) {
    int statusCode = response.getStatusCode();
    // Success 2xx
    if (statusCode >= 200 && statusCode < 300) {

      return serviceProfilerClient
          .reportUploadFinish(
              uploadContext.getProfileId(),
              uploadContext.getExtension(),
              response.getValue().getETag())
          .flatMap(
              uploadResponse -> {
                logger.debug("Completed upload request: {}", statusCode);
                if (uploadResponse != null) {
                  return Mono.just(
                      new UploadFinishArgs(
                          uploadResponse.getStampId(), uploadResponse.getAcceptedTime()));
                } else {
                  return Mono.error(new UploadFailedException("Report upload finish failed"));
                }
              });

    } else {
      logger.error("Upload of the trace file to block BLOB failed: {}", statusCode);
      return Mono.error(
          new UploadFailedException("Upload of the trace file to block BLOB failed " + statusCode));
    }
  }

  BlobUploadFromFileOptions createBlockBlobOptions(File file, UploadContext uploadContext) {
    HashMap<String, String> metadata = new HashMap<>();

    metadata.put(DATA_CUBE_META_NAME, uploadContext.getDataCube().toString().toLowerCase());
    metadata.put(MACHINE_NAME_META_NAME, uploadContext.getMachineName());
    metadata.put(
        START_TIME_META_NAME, TimestampContract.timestampToString(uploadContext.getSessionId()));
    metadata.put(PROGRAMMING_LANGUAGE_META_NAME, "Java");
    metadata.put(OS_PLATFORM_META_NAME, OsPlatformProvider.getOsPlatformDescription());
    metadata.put(TRACE_FILE_FORMAT_META_NAME, uploadContext.getFileFormat());

    if (roleName != null && !roleName.isEmpty()) {
      metadata.put(ROLE_NAME_META_NAME, roleName);
    }

    String fullFilePath = file.getAbsoluteFile().toString();

    return new BlobUploadFromFileOptions(fullFilePath)
        .setHeaders(new BlobHttpHeaders().setContentEncoding("gzip"))
        .setMetadata(metadata)
        .setParallelTransferOptions(
            new ParallelTransferOptions().setBlockSizeLong(UPLOAD_BLOCK_LENGTH));
  }

  /** Zip up profile. */
  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  private static File createZippedTraceFile(UploadContext uploadContext) throws IOException {
    File traceFile = uploadContext.getTraceFile();
    logger.debug("Trace file: {}", traceFile.toString());

    File targetFile =
        Files.createTempFile(traceFile.getParentFile().toPath(), traceFile.getName(), ".gz")
            .toFile();
    if (!retainJfrFile) {
      targetFile.deleteOnExit();
    }

    try (OutputStream target = new GZIPOutputStream(Files.newOutputStream(targetFile.toPath()))) {
      Files.copy(traceFile.toPath(), target);
    }

    return targetFile;
  }

  // Deleting file recursively.
  private static void deletePathRecursive(@Nullable File fileToDelete) throws IOException {
    if (fileToDelete != null && fileToDelete.exists()) {
      if (retainJfrFile) {
        logger.info("JFR file retained at: {}", fileToDelete.getAbsolutePath());
      } else {
        deletePathRecursive(fileToDelete.toPath());
      }
    }
  }

  // Deleting file recursively.
  private static void deletePathRecursive(Path path) throws IOException {
    try (Stream<Path> stream = Files.walk(path)) {
      stream
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(
              file -> {
                if (!file.delete()) {
                  logger.error("Failed to delete " + file.getAbsolutePath());
                }
              });
    }
  }
}
