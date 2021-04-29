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

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RP_AKS;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RP_APPSVC;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RP_FUNCTIONS;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.UNKNOWN;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.WEBSITE_HOME_STAMPNAME;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.WEBSITE_HOSTNAME;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.WEBSITE_SITE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AttachStatsbeatTest {

    private AttachStatsbeat attachStatsbeat;

    @Before
    public void setup() {
        StatsbeatModule.getInstance().initialize(new TelemetryClient(), DEFAULT_STATSBEAT_INTERVAL);
        attachStatsbeat = StatsbeatModule.getInstance().getAttachStatsbeat();
    }

    @Test
    public void testVirtualMachineResouceProviderId() throws IOException {
        assertEquals(attachStatsbeat.getResourceProviderId(), UNKNOWN);
        Path path = new File(Resources.getResource("metadata_instance_linux.json").getPath()).toPath();
        InputStream in = Files.newInputStream(path);
        BufferedSource source = Okio.buffer(Okio.source(in));
        String result = source.readUtf8();
        source.close();
        AzureMetadataService.parseJsonResponse(result);
        assertEquals(attachStatsbeat.getResourceProviderId(), "2a1216c3-a2a0-4fc5-a941-b1f5acde7051/65b2f83e-7bf1-4be3-bafc-3a4163265a52");
        assertEquals(attachStatsbeat.getOperatingSystem(), "Linux");
    }

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void testAppSvcResourceProviderId() {
        environmentVariables.set(WEBSITE_SITE_NAME, "test_site_name");
        environmentVariables.set(WEBSITE_HOME_STAMPNAME, "test_stamp_name");
        environmentVariables.set(WEBSITE_HOSTNAME, "test_hostname");
        attachStatsbeat.updateResourceProvider(RP_APPSVC);
        assertEquals(attachStatsbeat.getResourceProvider(), RP_APPSVC);
        assertEquals(attachStatsbeat.getResourceProviderId(), "test_site_name/test_stamp_name/test_hostname");
        environmentVariables.clear(WEBSITE_SITE_NAME, WEBSITE_HOME_STAMPNAME, WEBSITE_HOSTNAME);
        assertNull(System.getenv().get(WEBSITE_SITE_NAME));
        assertNull(System.getenv().get(WEBSITE_HOME_STAMPNAME));
        assertNull(System.getenv().get(WEBSITE_HOSTNAME));
    }

    @Test
    public void testFunctionsResourceProviderId() {
        environmentVariables.set(WEBSITE_HOSTNAME, "test_function_name");
        attachStatsbeat.updateResourceProvider(RP_FUNCTIONS);
        assertEquals(attachStatsbeat.getResourceProvider(), RP_FUNCTIONS);
        assertEquals(attachStatsbeat.getResourceProviderId(), "test_function_name");
        environmentVariables.clear(WEBSITE_HOSTNAME);
        assertNull(System.getenv().get(WEBSITE_HOSTNAME));
    }

    @Test
    public void testUnknownResourceProviderId() {
        assertEquals(attachStatsbeat.getResourceProvider(), UNKNOWN);
        assertEquals(attachStatsbeat.getResourceProviderId(), UNKNOWN);
    }

    @Test
    public void testAksResourceProviderId() {
        attachStatsbeat.updateResourceProvider(RP_AKS);
        assertEquals(attachStatsbeat.getResourceProvider(), RP_AKS);
        assertEquals(attachStatsbeat.getResourceProviderId(), UNKNOWN);
    }

    @Test
    public void testInterval() {
        assertEquals(attachStatsbeat.getInterval(), DEFAULT_STATSBEAT_INTERVAL);
    }
}
