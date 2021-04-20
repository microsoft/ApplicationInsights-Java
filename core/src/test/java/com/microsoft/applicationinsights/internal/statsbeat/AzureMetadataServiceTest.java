package com.microsoft.applicationinsights.internal.statsbeat;

import com.google.common.io.Resources;
import com.squareup.moshi.JsonDataException;
import okio.BufferedSource;
import okio.Okio;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;

import static org.junit.Assert.assertEquals;

public class AzureMetadataServiceTest {

    @Test
    public void testParseJsonResponseLinux() throws IOException, JsonDataException {
        Path path = new File(Resources.getResource("metadata_instance_linux.json").getPath()).toPath();
        InputStream in = Files.newInputStream(path);
        BufferedSource source = Okio.buffer(Okio.source(in));
        String result = source.readUtf8();
        source.close();
        AzureMetadataService.parseJsonResponse(result);
        MetadataInstanceResponse response = AttachStatsbeat.getMetadataInstanceResponse();
        assertEquals(response.getVmId(), "2a1216c3-a2a0-4fc5-a941-b1f5acde7051");
        assertEquals(response.getOsType(), "Linux");
        assertEquals(response.getResourceGroupName(), "heya-java-ipa");
        assertEquals(response.getSubscriptionId(), "65b2f83e-7bf1-4be3-bafc-3a4163265a52");
    }

    @Test
    public void testParseJsonResponseWindows() throws IOException, JsonDataException {
        Path path = new File(String.valueOf(Resources.getResource("metadata_instance_windows.json").getPath())).toPath();
        InputStream in = Files.newInputStream(path);
        BufferedSource source = Okio.buffer(Okio.source(in));
        String result = source.readUtf8();
        source.close();
        AzureMetadataService.parseJsonResponse(result);
        MetadataInstanceResponse response = AttachStatsbeat.getMetadataInstanceResponse();
        assertEquals(response.getVmId(), "2955a129-2323-4c1f-8918-994a7a83eefd");
        assertEquals(response.getOsType(), "Windows");
        assertEquals(response.getResourceGroupName(), "heya-java-ipa");
        assertEquals(response.getSubscriptionId(), "65b2f83e-7bf1-4be3-bafc-3a4163265a52");
    }
}
