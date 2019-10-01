package com.microsoft.applicationinsights.agentc.internal.diagnostics.log;

import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ApplicationInsightsJsonLayoutTests {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ApplicationInsightsJsonLayout ourLayout;
    private Map<String, Object> jsonMap = new HashMap<>();

    @Before
    public void setup() {
        ourLayout = new ApplicationInsightsJsonLayout();
    }

    @After
    public void tearDown() {
        ourLayout = null;
        jsonMap.clear();
    }

    @Test
    public void noEnvVarsMeansUnknownResourceId() {
        ourLayout.addCustomDataToJsonMap(jsonMap, null);
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(ResourceIdFinder.RESOURCE_ID_FIELD_NAME, ApplicationInsightsJsonLayout.UNKNOWN_VALUE));
    }

    @Test
    public void whenOnlyHostnameIsSetUseAsResourceId() {
        String value = "test-host.example.com";
        envVars.set(ResourceIdFinder.WEBSITE_HOSTNAME_ENV_VAR, value);
        ourLayout.addCustomDataToJsonMap(jsonMap, null);
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(ResourceIdFinder.RESOURCE_ID_FIELD_NAME, value));
    }

    @Test
    public void whenResourceIdFileIsNonEmptyUseAsResourceId() throws IOException {
        String value = "test-host2.example.com";
        envVars.set(ResourceIdFinder.WEBSITE_HOSTNAME_ENV_VAR, "wrong");
        File residFile = tempFolder.newFile(ResourceIdFinder.RESOURCE_ID_FILE_NAME);
        envVars.set(ResourceIdFinder.DIAGNOSTIC_LOGS_MOUNT_PATH_ENV_VAR, residFile.getParent());
        Files.write(Paths.get(residFile.getAbsolutePath()), value.getBytes(StandardCharsets.UTF_8));
        ourLayout.addCustomDataToJsonMap(jsonMap, null);
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(ResourceIdFinder.RESOURCE_ID_FIELD_NAME, value));
    }

    @Test
    public void ifResourceIdFileIsEmptyFallbackToHostname() throws IOException {
        String value = "test-host3.example.com";
        envVars.set(ResourceIdFinder.WEBSITE_HOSTNAME_ENV_VAR, value);
        File residFile = tempFolder.newFile(ResourceIdFinder.RESOURCE_ID_FILE_NAME);
        envVars.set(ResourceIdFinder.DIAGNOSTIC_LOGS_MOUNT_PATH_ENV_VAR, residFile.getParent());
        ourLayout.addCustomDataToJsonMap(jsonMap, null);
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(ResourceIdFinder.RESOURCE_ID_FIELD_NAME, value));
    }

    @Test
    public void isResourceIdFileAndHostnameAreEmptyIdIsUnknown() throws IOException {
        File residFile = tempFolder.newFile(ResourceIdFinder.RESOURCE_ID_FILE_NAME);
        envVars.set(ResourceIdFinder.DIAGNOSTIC_LOGS_MOUNT_PATH_ENV_VAR, residFile.getParent());
        ourLayout.addCustomDataToJsonMap(jsonMap, null);
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(ResourceIdFinder.RESOURCE_ID_FIELD_NAME, ApplicationInsightsJsonLayout.UNKNOWN_VALUE));
    }

}
