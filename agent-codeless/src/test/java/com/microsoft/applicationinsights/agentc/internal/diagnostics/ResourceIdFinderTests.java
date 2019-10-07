package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import com.microsoft.applicationinsights.agentc.internal.diagnostics.log.ApplicationInsightsJsonLayoutTests;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ResourceIdFinderTests {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceIdFinder finder;

    @Before
    public void setup() {
        finder = new ResourceIdFinder();
    }

    @After
    public void tearDown() {
        finder = null;
    }

    @Test
    public void noEnvVarsMeansUnknownResourceId() {
        assertNull(finder.getValue());
    }

    @Test
    public void whenOnlyHostnameIsSetUseAsResourceId() {
        String value = "test-host.example.com";
        envVars.set(ResourceIdFinder.WEBSITE_HOSTNAME_ENV_VAR, value);
        assertEquals(value, finder.getValue());
    }

    @Test
    public void whenResourceIdFileIsNonEmptyUseAsResourceId() throws IOException {
        String value = "test-host2.example.com";
        envVars.set(ResourceIdFinder.WEBSITE_HOSTNAME_ENV_VAR, "wrong");
        File residFile = tempFolder.newFile(ResourceIdFinder.RESOURCE_ID_FILE_NAME);
        envVars.set(ResourceIdFinder.DIAGNOSTIC_LOGS_MOUNT_PATH_ENV_VAR, residFile.getParent());
        Files.write(Paths.get(residFile.getAbsolutePath()), value.getBytes(StandardCharsets.UTF_8));
        assertEquals(value, finder.getValue());
    }

    @Test
    public void ifResourceIdFileIsEmptyFallbackToHostname() throws IOException {
        String value = "test-host3.example.com";
        envVars.set(ResourceIdFinder.WEBSITE_HOSTNAME_ENV_VAR, value);
        File residFile = tempFolder.newFile(ResourceIdFinder.RESOURCE_ID_FILE_NAME);
        envVars.set(ResourceIdFinder.DIAGNOSTIC_LOGS_MOUNT_PATH_ENV_VAR, residFile.getParent());
        assertEquals(value, finder.getValue());
    }

    @Test
    public void isResourceIdFileAndHostnameAreEmptyIdIsUnknown() throws IOException {
        File residFile = tempFolder.newFile(ResourceIdFinder.RESOURCE_ID_FILE_NAME);
        envVars.set(ResourceIdFinder.DIAGNOSTIC_LOGS_MOUNT_PATH_ENV_VAR, residFile.getParent());
        assertNull(finder.getValue());
    }
}
