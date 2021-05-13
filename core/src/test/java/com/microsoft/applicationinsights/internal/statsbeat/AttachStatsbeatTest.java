package com.microsoft.applicationinsights.internal.statsbeat;

import com.google.common.io.Resources;
import com.microsoft.applicationinsights.TelemetryClient;
import okio.BufferedSource;
import okio.Okio;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_OS;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_RP;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RP_AKS;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RP_APPSVC;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RP_FUNCTIONS;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.UNKNOWN;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.WEBSITE_HOME_STAMPNAME;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.WEBSITE_HOSTNAME;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.WEBSITE_SITE_NAME;
import static org.junit.Assert.assertEquals;

public class AttachStatsbeatTest {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    private AttachStatsbeat attachStatsbeat;

    @Before
    public void setup() {
        attachStatsbeat = new AttachStatsbeat(new TelemetryClient(), DEFAULT_STATSBEAT_INTERVAL);
    }

    @Test
    public void testVirtualMachineResourceProviderId() throws IOException {
        assertEquals(UNKNOWN, attachStatsbeat.getResourceProviderId());
        Path path = new File(Resources.getResource("metadata_instance_linux.json").getPath()).toPath();
        InputStream in = Files.newInputStream(path);
        BufferedSource source = Okio.buffer(Okio.source(in));
        String result = source.readUtf8();
        source.close();
        CustomDimensions customDimensions = new CustomDimensions();
        AzureMetadataService azureMetadataService = new AzureMetadataService(attachStatsbeat, customDimensions);
        azureMetadataService.parseJsonResponse(result);
        assertEquals(attachStatsbeat.getResourceProviderId(), "2a1216c3-a2a0-4fc5-a941-b1f5acde7051/65b2f83e-7bf1-4be3-bafc-3a4163265a52");
        assertEquals("Linux", customDimensions.getProperties().get(CUSTOM_DIMENSIONS_OS));
    }

    @Test
    public void testAppSvcResourceProviderId() {
        envVars.set(WEBSITE_SITE_NAME, "test_site_name");
        envVars.set(WEBSITE_HOME_STAMPNAME, "test_stamp_name");
        envVars.set(WEBSITE_HOSTNAME, "test_hostname");

        assertEquals("test_site_name/test_stamp_name/test_hostname", AttachStatsbeat.initResourceProviderId(RP_APPSVC, null));
    }

    @Test
    public void testFunctionsResourceProviderId() {
        envVars.set(WEBSITE_HOSTNAME, "test_function_name");

        assertEquals("test_function_name", AttachStatsbeat.initResourceProviderId(RP_FUNCTIONS, null));
    }

    @Test
    public void testAksResourceProviderId() {
        assertEquals(UNKNOWN, AttachStatsbeat.initResourceProviderId(RP_AKS, null));
    }

    @Test
    public void testInterval() {
        assertEquals(DEFAULT_STATSBEAT_INTERVAL, attachStatsbeat.getInterval());
    }

    @Test
    public void testUnknownResourceProviderId() {
        assertEquals(UNKNOWN, new CustomDimensions().getProperties().get(CUSTOM_DIMENSIONS_RP));
        assertEquals(UNKNOWN, attachStatsbeat.getResourceProviderId());
    }
}
