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

package com.microsoft.applicationinsights.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import okio.BufferedSource;
import okio.Okio;
import org.junit.jupiter.api.Test;

public class AzureMetadataServiceTest {

  @Test
  public void testParseJsonResponseLinux() throws IOException {
    Path path =
        new File(getClass().getClassLoader().getResource("metadata_instance_linux.json").getPath())
            .toPath();
    InputStream in = Files.newInputStream(path);
    BufferedSource source = Okio.buffer(Okio.source(in));
    String result = source.readUtf8();
    source.close();

    AttachStatsbeat attachStatsbeat = new AttachStatsbeat(new CustomDimensions());
    AzureMetadataService azureMetadataService =
        new AzureMetadataService(attachStatsbeat, new CustomDimensions());
    azureMetadataService.parseJsonResponse(result);

    MetadataInstanceResponse response = attachStatsbeat.getMetadataInstanceResponse();
    assertThat(response.getVmId()).isEqualTo("2a1216c3-a2a0-4fc5-a941-b1f5acde7051");
    assertThat(response.getOsType()).isEqualTo("Linux");
    assertThat(response.getResourceGroupName()).isEqualTo("heya-java-ipa");
    assertThat(response.getSubscriptionId()).isEqualTo("65b2f83e-7bf1-4be3-bafc-3a4163265a52");
  }

  @Test
  public void testParseJsonResponseWindows() throws IOException {
    Path path =
        new File(
                getClass().getClassLoader().getResource("metadata_instance_windows.json").getPath())
            .toPath();
    InputStream in = Files.newInputStream(path);
    BufferedSource source = Okio.buffer(Okio.source(in));
    String result = source.readUtf8();
    source.close();

    AttachStatsbeat attachStatsbeat = new AttachStatsbeat(new CustomDimensions());
    AzureMetadataService azureMetadataService =
        new AzureMetadataService(attachStatsbeat, new CustomDimensions());
    azureMetadataService.parseJsonResponse(result);

    MetadataInstanceResponse response = attachStatsbeat.getMetadataInstanceResponse();
    assertThat(response.getVmId()).isEqualTo("2955a129-2323-4c1f-8918-994a7a83eefd");
    assertThat(response.getOsType()).isEqualTo("Windows");
    assertThat(response.getResourceGroupName()).isEqualTo("heya-java-ipa");
    assertThat(response.getSubscriptionId()).isEqualTo("65b2f83e-7bf1-4be3-bafc-3a4163265a52");
  }
}
