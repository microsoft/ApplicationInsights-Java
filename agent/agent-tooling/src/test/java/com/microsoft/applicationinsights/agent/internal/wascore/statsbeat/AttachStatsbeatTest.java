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

package com.microsoft.applicationinsights.agent.internal.wascore.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import okio.BufferedSource;
import okio.Okio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class AttachStatsbeatTest {

  @SystemStub EnvironmentVariables envVars = new EnvironmentVariables();

  private AttachStatsbeat attachStatsbeat;

  @BeforeEach
  public void setup() {
    attachStatsbeat = new AttachStatsbeat(new CustomDimensions());
  }

  @Test
  public void testVirtualMachineResourceProviderId() throws IOException {
    assertThat(attachStatsbeat.getResourceProviderId()).isEqualTo("unknown");
    Path path =
        new File(getClass().getClassLoader().getResource("metadata_instance_linux.json").getPath())
            .toPath();
    InputStream in = Files.newInputStream(path);
    BufferedSource source = Okio.buffer(Okio.source(in));
    String result = source.readUtf8();
    source.close();
    CustomDimensions customDimensions = new CustomDimensions();
    AzureMetadataService azureMetadataService =
        new AzureMetadataService(attachStatsbeat, customDimensions);
    azureMetadataService.parseJsonResponse(result);
    assertThat("2a1216c3-a2a0-4fc5-a941-b1f5acde7051/65b2f83e-7bf1-4be3-bafc-3a4163265a52")
        .isEqualTo(attachStatsbeat.getResourceProviderId());

    Map<String, String> properties = new HashMap<>();
    customDimensions.populateProperties(properties, null);
    assertThat(properties.get("os")).isEqualTo("Linux");
  }

  @Test
  public void testAppSvcResourceProviderId() {
    envVars.set("WEBSITE_SITE_NAME", "test_site_name");
    envVars.set("WEBSITE_HOME_STAMPNAME", "test_stamp_name");

    assertThat(AttachStatsbeat.initResourceProviderId(ResourceProvider.RP_APPSVC, null))
        .isEqualTo("test_site_name/test_stamp_name");
  }

  @Test
  public void testFunctionsResourceProviderId() {
    envVars.set("WEBSITE_HOSTNAME", "test_function_name");
    assertThat(AttachStatsbeat.initResourceProviderId(ResourceProvider.RP_FUNCTIONS, null))
        .isEqualTo("test_function_name");
  }

  @Test
  public void testAksResourceProviderId() {
    assertThat(AttachStatsbeat.initResourceProviderId(ResourceProvider.RP_AKS, null))
        .isEqualTo("unknown");
  }

  @Test
  public void testUnknownResourceProviderId() {
    assertThat(new CustomDimensions().getResourceProvider()).isEqualTo(ResourceProvider.UNKNOWN);
    assertThat(attachStatsbeat.getResourceProviderId()).isEqualTo("unknown");
  }
}
