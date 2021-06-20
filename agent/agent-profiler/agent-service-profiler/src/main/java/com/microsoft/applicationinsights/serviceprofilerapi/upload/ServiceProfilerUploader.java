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
package com.microsoft.applicationinsights.serviceprofilerapi.upload;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.microsoft.applicationinsights.profileUploader.ServiceProfilerIndex;
import com.microsoft.applicationinsights.profileUploader.UploadResult;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ClientClosedException;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.ArtifactAcceptedResponse;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobAccessPass;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobMetadataConstants;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.TimestampContract;
import com.microsoft.applicationinsights.serviceprofilerapi.client.uploader.OsPlatformProvider;
import com.microsoft.applicationinsights.serviceprofilerapi.client.uploader.UploadContext;
import com.microsoft.applicationinsights.serviceprofilerapi.client.uploader.UploadFinishArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

/**
 * Uploads profiles to the service profiler endpoint
 */
public class ServiceProfilerUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProfilerUploader.class);
    private static final Random RANDOM = new Random();
    private static final long UPLOAD_BLOCK_LENGTH = 8 * 1024 * 1024;

    private final ServiceProfilerClientV2 serviceProfilerClient;
    private final String machineName;
    private final Supplier<String> appIdSupplier;
    private final String processId;
    private final String roleName;

    public ServiceProfilerUploader(
            ServiceProfilerClientV2 serviceProfilerClient,
            String machineName,
            String processId,
            Supplier<String> appIdSupplier,
            String roleName) {
        this.appIdSupplier = appIdSupplier;
        this.machineName = machineName;
        this.serviceProfilerClient = serviceProfilerClient;
        this.processId = processId;
        this.roleName = roleName;
    }

    /**
     * Upload a given JFR file and return associated metadata of the uploaded profile
     */
    public Mono<UploadResult> uploadJfrFile(
            String triggerName,
            long timestamp,
            File file,
            double cpuUsage,
            double memoryUsage) {
        String appId = appIdSupplier.get();
        if (appId == null || appId.isEmpty()) {
            LOGGER.error("Failed to upload due to lack of appId");
            return Mono.error(new UploadFailedException("Failed to upload due to lack of appId"));
        }
        UUID profileId = UUID.randomUUID();

        UploadContext uploadContext = new UploadContext(
                machineName,
                UUID.fromString(appId),
                timestamp,
                file,
                profileId
        );

        // upload trace to service profiler
        return uploadTrace(uploadContext)
                .map(done -> {
                    // return associated profile metadata
                    String fileId = createId(9);
                    String formattedTimestamp = TimestampContract.padNanos(done.getTimeStamp());
                    return new UploadResult(
                            new ServiceProfilerIndex(
                                    triggerName,
                                    fileId,
                                    done.getStampId(),
                                    UUID.fromString(appId),
                                    formattedTimestamp,
                                    uploadContext.getMachineName(),
                                    OsPlatformProvider.getOSPlatformDescription(),
                                    processId,
                                    "Profile",
                                    profileId.toString(),
                                    "jfr",
                                    cpuUsage,
                                    memoryUsage
                            )
                    );
                });
    }

    public static String createId(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Upload profile to service profiler
     */
    public Mono<UploadFinishArgs> uploadTrace(UploadContext uploadContext) {

        File zippedTraceFile = null;

        try {
            // Zip up profile
            zippedTraceFile = createZippedTraceFile(uploadContext);

            // Obtain permission to upload profile
            BlobAccessPass uploadPass = serviceProfilerClient.getUploadAccess(uploadContext.getProfileId());
            if (uploadPass == null) {
                close(zippedTraceFile);
                return Mono.error(new UploadFailedException("Failed to obtain upload pass"));
            }

            // perform upload then finally close file
            File finalZippedTraceFile = zippedTraceFile;
            return performUpload(uploadContext, uploadPass, zippedTraceFile)
                    .doFinally((signal) -> close(finalZippedTraceFile));
        } catch (Exception e) {
            LOGGER.error("Upload of the trace file failed", e);
            close(zippedTraceFile);
            return Mono.error(new UploadFailedException(e));
        }
    }

    protected Mono<UploadFinishArgs> performUpload(UploadContext uploadContext, BlobAccessPass uploadPass, File file) throws IOException {
        return uploadToSasLink(uploadPass, uploadContext, file)
                .flatMap(response -> {
                    try {
                        return Mono.just(reportUploadComplete(uploadContext.getProfileId(), response));
                    } catch (UploadFailedException e) {
                        return Mono.error(e);
                    }
                });
    }

    /**
     * Upload the given file to a blob storage defined by a sas link
     */
    private Mono<Response<BlockBlobItem>> uploadToSasLink(BlobAccessPass uploadPass, UploadContext uploadContext, File file) throws MalformedURLException {
        URL sasUrl = new URL(uploadPass.getUriWithSasToken());
        LOGGER.debug("SAS token: {}", uploadPass.getUriWithSasToken());

        BlobUploadFromFileOptions options = createBlockBlobOptions(file, uploadContext);
        BlobContainerAsyncClient blobContainerClient = new BlobContainerClientBuilder().endpoint(sasUrl.toString()).buildAsyncClient();

        BlobAsyncClient blobClient = blobContainerClient.getBlobAsyncClient(uploadPass.getBlobName());
        return blobClient
                .uploadFromFileWithResponse(options)
                .doFinally((done) -> LOGGER.info("upload done"));
    }

    private void close(File zippedTraceFile) {
        try {
            deletePathRecursive(zippedTraceFile);
        } catch (Exception e) {
            LOGGER.warn("An error occurred when closing the zipped trace file", e);
        }
    }

    /**
     * Report the success of an upload or throw an exception
     */
    protected UploadFinishArgs reportUploadComplete(UUID profileId, Response<BlockBlobItem> response) throws UploadFailedException {
        int statusCode = response.getStatusCode();
        // Success 2xx
        if (statusCode >= 200 && statusCode < 300) {
            ArtifactAcceptedResponse uploadResponse;
            try {
                uploadResponse = serviceProfilerClient.reportUploadFinish(profileId, response.getValue().getETag());
            } catch (URISyntaxException | ClientClosedException | IOException e) {
                throw new UploadFailedException(e);
            }

            LOGGER.debug("Completed upload request: {}", statusCode);

            if (uploadResponse != null) {
                return new UploadFinishArgs(uploadResponse.getStampId(), uploadResponse.getAcceptedTime());
            } else {
                throw new UploadFailedException("Report upoad finish failed");
            }
        } else {
            LOGGER.error("Upload of the trace file to block BLOB failed: {}", statusCode);
            throw new UploadFailedException("Upload of the trace file to block BLOB failed " + statusCode);
        }
    }

    BlobUploadFromFileOptions createBlockBlobOptions(File file, UploadContext uploadContext) {
        HashMap<String, String> metadata = new HashMap<>();

        metadata.put(BlobMetadataConstants.DATA_CUBE_META_NAME, uploadContext.getDataCube().toString().toLowerCase());
        metadata.put(BlobMetadataConstants.MACHINE_NAME_META_NAME, uploadContext.getMachineName());
        metadata.put(BlobMetadataConstants.START_TIME_META_NAME, TimestampContract.timestampToString(uploadContext.getSessionId()));
        metadata.put(BlobMetadataConstants.PROGRAMMING_LANGUAGE_META_NAME, "Java");
        metadata.put(BlobMetadataConstants.OS_PLATFORM_META_NAME, OsPlatformProvider.getOSPlatformDescription());
        metadata.put(BlobMetadataConstants.TRACE_FILE_FORMAT_META_NAME, "jfr");

        if (roleName != null && !roleName.isEmpty()) {
            metadata.put(BlobMetadataConstants.ROLE_NAME_META_NAME, roleName);
        }

        String fullFilePath = file
                .getAbsoluteFile()
                .toString();

        return new BlobUploadFromFileOptions(fullFilePath)
                .setHeaders(new BlobHttpHeaders().setContentEncoding("gzip"))
                .setMetadata(metadata)
                .setParallelTransferOptions(new ParallelTransferOptions().setBlockSizeLong(UPLOAD_BLOCK_LENGTH));
    }

    /**
     * Zip up profile
     */
    private File createZippedTraceFile(UploadContext uploadContext) throws IOException {
        File traceFile = uploadContext.getTraceFile();
        LOGGER.debug("Trace file: {}", traceFile.toString());

        File targetFile = Files.createTempFile(traceFile.getName(), ".gz").toFile();
        targetFile.deleteOnExit();
        try (OutputStream target = new GZIPOutputStream(new FileOutputStream(targetFile))) {
            Files.copy(traceFile.toPath(), target);
        }

        return targetFile;
    }

    // Deleting file recursively.
    private void deletePathRecursive(File fileToDelete) throws IOException {
        if (fileToDelete != null && fileToDelete.exists()) {
            deletePathRecursive(fileToDelete.toPath());
        }
    }

    // Deleting file recursively.
    private void deletePathRecursive(Path path) throws IOException {
        if (path != null) {
            Files
                    .walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) {
                            LOGGER.error("Failed to delete " + file.getAbsolutePath());
                        }
                    });
        }
    }
}
