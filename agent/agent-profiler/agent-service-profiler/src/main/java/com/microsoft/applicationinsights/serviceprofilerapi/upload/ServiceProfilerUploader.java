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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

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
import com.microsoft.azure.storage.blob.AnonymousCredentials;
import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.CommonRestResponse;
import com.microsoft.azure.storage.blob.Metadata;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.TransferManager;
import com.microsoft.azure.storage.blob.TransferManagerUploadToBlockBlobOptions;
import com.microsoft.azure.storage.blob.models.BlobHTTPHeaders;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uploads profiles to the service profiler endpoint
 */
public class ServiceProfilerUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProfilerUploader.class);
    private static final Random RANDOM = new Random();
    private static final int UPLOAD_BLOCK_LENGTH = 8 * 1024 * 1024;

    private final ServiceProfilerClientV2 serviceProfilerClient;
    private final String machineName;
    private final Supplier<String> appIdSupplier;
    private final String processId;
    private final URI customUploadUrl;

    public ServiceProfilerUploader(
            ServiceProfilerClientV2 serviceProfilerClient,
            String machineName,
            String processId,
            Supplier<String> appIdSupplier) {
        this(
                serviceProfilerClient,
                machineName,
                processId,
                appIdSupplier,
                System.getenv("CUSTOM_UPLOAD_URL")
        );
    }

    public ServiceProfilerUploader(
            ServiceProfilerClientV2 serviceProfilerClient,
            String machineName,
            String processId,
            Supplier<String> appIdSupplier,
            String customUploadUrl) {
        this.appIdSupplier = appIdSupplier;
        this.machineName = machineName;
        this.serviceProfilerClient = serviceProfilerClient;
        this.processId = processId;

        if (customUploadUrl != null && !customUploadUrl.isEmpty()) {
            this.customUploadUrl = URI.create(customUploadUrl);
        } else {
            this.customUploadUrl = null;
        }
    }

    /**
     * Upload a given JFR file and return associated metadata of the uploaded profile
     */
    public Single<UploadResult> uploadJfrFile(
            String triggerName,
            long timestamp,
            File file,
            double cpuUsage,
            double memoryUsage) {
        String appId = appIdSupplier.get();
        if (appId == null || appId.isEmpty()) {
            LOGGER.error("Failed to upload due to lack of appId");
            return Single.error(new UploadFailedException("Failed to upload due to lack of appId"));
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
    public Single<UploadFinishArgs> uploadTrace(UploadContext uploadContext) {

        File zippedTraceFile = null;

        try {
            // Zip up profile
            zippedTraceFile = createZippedTraceFile(uploadContext);

            // If required upload trace to a custom storage location
            if (customUploadUrl != null) {
                uploadToCustomStore(uploadContext, zippedTraceFile);
            }

            // Obtain permission to upload profile
            BlobAccessPass uploadPass = serviceProfilerClient.getUploadAccess(uploadContext.getProfileId());
            if (uploadPass == null) {
                close(zippedTraceFile, null);
                return Single.error(new UploadFailedException("Failed to obtain upload pass"));
            }

            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(zippedTraceFile.toPath());
            File finalZippedTraceFile = zippedTraceFile;

            // perform upload then finally close file
            return performUpload(uploadContext, uploadPass, fileChannel)
                    .doFinally(() -> close(finalZippedTraceFile, fileChannel));
        } catch (Exception e) {
            LOGGER.error("Upload of the trace file failed", e);
            close(zippedTraceFile, null);
            return Single.error(new UploadFailedException(e));
        }
    }

    // Upload file into blob storage container
    protected void uploadToCustomStore(UploadContext uploadContext, File zippedTraceFile) throws IOException {
        // fire and forget as its only temporary
        AsynchronousFileChannel file = AsynchronousFileChannel.open(zippedTraceFile.toPath());
        File traceFile = uploadContext.getTraceFile();
        uploadToAltBlobStore(customUploadUrl, traceFile, uploadContext, file)
                .doFinally(file::close)
                .subscribe(done -> LOGGER.info("Upload to custom address complete"),
                        error -> LOGGER.error("Error while uploading file to blob store", error));
    }

    protected Single<UploadFinishArgs> performUpload(UploadContext uploadContext, BlobAccessPass uploadPass, AsynchronousFileChannel fileChannel) throws IOException {
        return uploadFile(uploadContext, uploadPass, fileChannel)
                .map(response -> reportUploadComplete(uploadContext.getProfileId(), response));
    }

    protected Single<CommonRestResponse> uploadFile(UploadContext uploadContext, BlobAccessPass uploadPass, AsynchronousFileChannel fileChannel)
            throws IOException {
        LOGGER.debug("SAS token: {}", uploadPass.getUriWithSasToken());
        URL sasUrl = new URL(uploadPass.getUriWithSasToken());
        return uploadToSasLink(sasUrl, uploadContext, fileChannel);
    }

    /**
     * Upload the given file to a blob storage defined by a sas link
     */
    private Single<CommonRestResponse> uploadToSasLink(URL sasUrl, UploadContext uploadContext, AsynchronousFileChannel fileChannel) throws IOException {
        BlockBlobURL blobURL = new BlockBlobURL(sasUrl,
                BlockBlobURL.createPipeline(new AnonymousCredentials(), new PipelineOptions()));

        TransferManagerUploadToBlockBlobOptions options = createBlockBlobOptions(uploadContext);

        return TransferManager
                .uploadFileToBlockBlob(fileChannel, blobURL, UPLOAD_BLOCK_LENGTH, null, options)
                .doFinally(() -> LOGGER.info("upload done"));
    }

    public Maybe<CommonRestResponse> uploadToAltBlobStore(URI customUploadUrl, File file, UploadContext uploadContext,
                                                          AsynchronousFileChannel fileChannel) {
        try {
            if (customUploadUrl != null) {

                // add file name to sas link
                String url = customUploadUrl.toURL().toString();
                String[] parts = url.split("\\?");
                if (parts.length == 2) {
                    String urlWithFile = parts[0] + "/" + file.getName() + "?" + parts[1];
                    // No creds so assume a sas link
                    return uploadToSasLink(new URL(urlWithFile), uploadContext, fileChannel)
                            .toMaybe();
                } else {
                    throw new MalformedURLException("Malformed custom SAS link");
                }
            } else {
                return Maybe.empty();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to upload", e);
            return Maybe.error(e);
        }
    }

    private void close(File zippedTraceFile, AsynchronousFileChannel fileChannel) {
        try {
            if (fileChannel != null) {
                fileChannel.close();
            }
            deletePathRecursive(zippedTraceFile);
        } catch (Exception e) {
            LOGGER.warn("An error occurred when closing the zipped trace file", e);
        }
    }

    /**
     * Report the success of an upload or throw an exception
     */
    protected UploadFinishArgs reportUploadComplete(UUID profileId, CommonRestResponse response) throws UploadFailedException {
        int statusCode = response.statusCode();
        // Success 2xx
        if (statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES) {
            ArtifactAcceptedResponse uploadResponse;
            try {
                uploadResponse = serviceProfilerClient.reportUploadFinish(profileId, response.eTag());
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
            LOGGER.error("Upload of the trace file to block BLOB failed: {} {}", statusCode, response.response().body());
            throw new UploadFailedException("Upload of the trace file to block BLOB failed " + statusCode);
        }
    }

    private TransferManagerUploadToBlockBlobOptions createBlockBlobOptions(UploadContext uploadContext) {
        BlobHTTPHeaders httpHeaders = new BlobHTTPHeaders()
                .withBlobContentEncoding("gzip");

        Metadata metadata = new Metadata();

        metadata.put(BlobMetadataConstants.DATA_CUBE_META_NAME, uploadContext.getDataCube().toString().toLowerCase());
        metadata.put(BlobMetadataConstants.MACHINE_NAME_META_NAME, uploadContext.getMachineName());
        metadata.put(BlobMetadataConstants.START_TIME_META_NAME, TimestampContract.timestampToString(uploadContext.getSessionId()));
        metadata.put(BlobMetadataConstants.PROGRAMMING_LANGUAGE_META_NAME, "Java");
        metadata.put(BlobMetadataConstants.OS_PLATFORM_META_NAME, OsPlatformProvider.getOSPlatformDescription());
        metadata.put(BlobMetadataConstants.TRACE_FILE_FORMAT_META_NAME, "Netperf");

        return new TransferManagerUploadToBlockBlobOptions(null,
                httpHeaders, metadata, null, null);
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
