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

import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.microsoft.applicationinsights.profileUploader.ServiceProfilerIndex;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ClientClosedException;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.ArtifactAcceptedResponse;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobAccessPass;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobMetadataConstants;
import com.microsoft.applicationinsights.serviceprofilerapi.client.uploader.UploadContext;
import com.microsoft.applicationinsights.serviceprofilerapi.client.uploader.UploadFinishArgs;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceProfilerUploaderTest {
    @Test
    public void uploadFileGoodPathReturnsExpectedResponse() throws IOException {

        ServiceProfilerClientV2 serviceProfilerClient = stubServiceProfilerClient();

        File tmpFile = createFakeJfrFile();
        UUID appId = UUID.randomUUID();

        ServiceProfilerUploader serviceProfilerUploader = new ServiceProfilerUploader(
                serviceProfilerClient,
                "a-machine-name",
                "a-process-id",
                appId::toString,
                "a-role-name"
        ) {
            @Override
            protected Mono<UploadFinishArgs> performUpload(UploadContext uploadContext, BlobAccessPass uploadPass, File file) {
                return Mono.just(new UploadFinishArgs("a-stamp-id", "a-timestamp"));
            }
        };

        serviceProfilerUploader
                .uploadJfrFile(
                        "a-trigger",
                        321,
                        tmpFile,
                        0.0,
                        0.0)
                .subscribe(
                        result -> {
                            Assert.assertEquals("a-stamp-id",
                                    result.getServiceProfilerIndex().getProperties().get(ServiceProfilerIndex.SERVICE_PROFILER_STAMPID_PROPERTY_NAME));

                            Assert.assertEquals("a-machine-name",
                                    result.getServiceProfilerIndex().getProperties().get(ServiceProfilerIndex.SERVICE_PROFILER_MACHINENAME_PROPERTY_NAME));

                            Assert.assertEquals("a-timestamp",
                                    result.getServiceProfilerIndex().getProperties().get(ServiceProfilerIndex.SERVICE_PROFILER_ETLFILESESSIONID_PROPERTY_NAME));

                            Assert.assertEquals(appId.toString(),
                                    result.getServiceProfilerIndex().getProperties().get(ServiceProfilerIndex.SERVICE_PROFILER_DATACUBE_PROPERTY_NAME));
                        });
    }

    @Test
    public void roleNameIsCorrectlyAddedToMetaData() throws IOException {

        ServiceProfilerClientV2 serviceProfilerClient = stubServiceProfilerClient();

        File tmpFile = createFakeJfrFile();
        UUID appId = UUID.randomUUID();

        BlobUploadFromFileOptions blobOptions = new ServiceProfilerUploader(
                serviceProfilerClient,
                "a-machine-name",
                "a-process-id",
                appId::toString,
                "a-role-name"
        ).createBlockBlobOptions(
                tmpFile,
                new UploadContext("a-machine-name", UUID.randomUUID(), 1, tmpFile, UUID.randomUUID())
        );

        // Role name is set correctly
        Assert.assertEquals("a-role-name", blobOptions.getMetadata().get(BlobMetadataConstants.ROLE_NAME_META_NAME));


        blobOptions = new ServiceProfilerUploader(
                serviceProfilerClient,
                "a-machine-name",
                "a-process-id",
                appId::toString,
                null
        ).createBlockBlobOptions(
                tmpFile,
                new UploadContext("a-machine-name", UUID.randomUUID(), 1, tmpFile, UUID.randomUUID())
        );

        // Null role name tag is not added
        Assert.assertNull(blobOptions.getMetadata().get(BlobMetadataConstants.ROLE_NAME_META_NAME));

    }

    @Test
    public void uploadWithoutAFileThrows() {

        ServiceProfilerClientV2 serviceProfilerClient = stubServiceProfilerClient();

        UUID appId = UUID.randomUUID();

        ServiceProfilerUploader serviceProfilerUploader = new ServiceProfilerUploader(serviceProfilerClient, "a-machine-name", "a-process-id", appId::toString, "a-role-name");

        AtomicBoolean threw = new AtomicBoolean(false);
        serviceProfilerUploader
                .uploadJfrFile(
                        "a-trigger",
                        321,
                        new File("not-a-file"),
                        0.0,
                        0.0
                )
                .subscribe(result -> {
                }, e -> threw.set(true));

        Assert.assertTrue("Did not throw", threw.get());
    }

    private File createFakeJfrFile() throws IOException {
        File tmpFile = File.createTempFile("a-jfr-file", "jfr");
        FileOutputStream fos = new FileOutputStream(tmpFile);
        fos.write("foobar".getBytes());
        fos.close();
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    public static ServiceProfilerClientV2 stubServiceProfilerClient() {
        return new ServiceProfilerClientV2() {

            @Override
            public BlobAccessPass getUploadAccess(UUID profileId) {
                return new BlobAccessPass("https://localhost:99999/a-blob-uri", null, "a-sas-token");
            }

            @Override
            public ArtifactAcceptedResponse reportUploadFinish(UUID profileId, String etag) throws URISyntaxException, UnsupportedCharsetException, ClientClosedException {
                return null;
            }

            @Override
            public String getSettings(Date oldTimeStamp) {
                return null;
            }
        };
    }

}
