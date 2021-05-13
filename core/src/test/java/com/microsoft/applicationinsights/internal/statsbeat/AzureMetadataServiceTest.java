package com.microsoft.applicationinsights.internal.statsbeat;

import com.google.common.io.Resources;
import com.microsoft.applicationinsights.TelemetryClient;
import com.squareup.moshi.JsonDataException;
import okio.BufferedSource;
import okio.Okio;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.FEATURE_STATSBEAT_INTERVAL;
import static org.junit.Assert.assertEquals;

public class AzureMetadataServiceTest {

    @Test
    public void testParseJsonResponseLinux() throws IOException, JsonDataException {
        Path path = new File(Resources.getResource("metadata_instance_linux.json").getPath()).toPath();
        InputStream in = Files.newInputStream(path);
        BufferedSource source = Okio.buffer(Okio.source(in));
        String result = source.readUtf8();
        source.close();

        AttachStatsbeat attachStatsbeat = new AttachStatsbeat(new TelemetryClient(), DEFAULT_STATSBEAT_INTERVAL);
        AzureMetadataService azureMetadataService = new AzureMetadataService(attachStatsbeat, new CustomDimensions());
        azureMetadataService.parseJsonResponse(result);

        MetadataInstanceResponse response = attachStatsbeat.getMetadataInstanceResponse();
        assertEquals("2a1216c3-a2a0-4fc5-a941-b1f5acde7051", response.getVmId());
        assertEquals("Linux", response.getOsType());
        assertEquals("heya-java-ipa", response.getResourceGroupName());
        assertEquals("65b2f83e-7bf1-4be3-bafc-3a4163265a52", response.getSubscriptionId());
    }

    @Test
    public void testParseJsonResponseWindows() throws IOException, JsonDataException {
        Path path = new File(String.valueOf(Resources.getResource("metadata_instance_windows.json").getPath())).toPath();
        InputStream in = Files.newInputStream(path);
        BufferedSource source = Okio.buffer(Okio.source(in));
        String result = source.readUtf8();
        source.close();

        AttachStatsbeat attachStatsbeat = new AttachStatsbeat(new TelemetryClient(), DEFAULT_STATSBEAT_INTERVAL);
        AzureMetadataService azureMetadataService = new AzureMetadataService(attachStatsbeat, new CustomDimensions());
        azureMetadataService.parseJsonResponse(result);

        MetadataInstanceResponse response = attachStatsbeat.getMetadataInstanceResponse();
        assertEquals("2955a129-2323-4c1f-8918-994a7a83eefd", response.getVmId());
        assertEquals("Windows", response.getOsType());
        assertEquals("heya-java-ipa", response.getResourceGroupName());
        assertEquals("65b2f83e-7bf1-4be3-bafc-3a4163265a52", response.getSubscriptionId());
    }
}
