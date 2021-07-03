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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.microsoft.applicationinsights.agent.internal.common.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AzureMetadataService implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AzureMetadataService.class);

  private static final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor(
          ThreadPoolUtils.createDaemonThreadFactory(AzureMetadataService.class));

  private static final String API_VERSION =
      "api-version=2017-08-01"; // this version has the smallest payload.
  private static final String JSON_FORMAT = "format=json";
  private static final String BASE_URL = "http://169.254.169.254/metadata/instance/compute";
  private static final String ENDPOINT = BASE_URL + "?" + API_VERSION + "&" + JSON_FORMAT;

  private static final JsonAdapter<MetadataInstanceResponse> jsonAdapter =
      new Moshi.Builder().build().adapter(MetadataInstanceResponse.class);

  private final AttachStatsbeat attachStatsbeat;
  private final CustomDimensions customDimensions;

  AzureMetadataService(AttachStatsbeat attachStatsbeat, CustomDimensions customDimensions) {
    this.attachStatsbeat = attachStatsbeat;
    this.customDimensions = customDimensions;
  }

  void scheduleWithFixedDelay(long interval) {
    // Querying Azure Metadata Service is required for every 15 mins since VM id will get updated
    // frequently.
    // Starting and restarting a VM will generate a new VM id each time.
    scheduledExecutor.scheduleWithFixedDelay(this, interval, interval, TimeUnit.SECONDS);
  }

  // only used by tests
  void updateMetadata(String response) throws IOException {
    updateMetadata(jsonAdapter.fromJson(response));
  }

  // visible for testing
  private void updateMetadata(MetadataInstanceResponse metadataInstanceResponse) {
    attachStatsbeat.updateMetadataInstance(metadataInstanceResponse);
    customDimensions.setResourceProvider(ResourceProvider.RP_VM);

    // osType from the Azure Metadata Service has a higher precedence over the running app’s
    // operating system.
    String osType = metadataInstanceResponse.getOsType();
    switch (osType) {
      case "Windows":
        customDimensions.setOperatingSystem(OperatingSystem.OS_WINDOWS);
        break;
      case "Linux":
        customDimensions.setOperatingSystem(OperatingSystem.OS_LINUX);
        break;
      default:
        // unknown, ignore
    }
  }

  @Override
  public void run() {
    HttpRequest request = new HttpRequest(HttpMethod.GET, ENDPOINT);
    request.setHeader("Metadata", "true");
    HttpResponse response;
    try {
      response = LazyHttpClient.getInstance().send(request).block();
    } catch (RuntimeException e) {
      logger.debug(
          "Shutting down AzureMetadataService scheduler: is not running on Azure VM or VMSS");
      logger.trace(e.getMessage(), e);
      scheduledExecutor.shutdown();
      return;
    }

    if (response == null) {
      // this shouldn't happen, the mono should complete with a response or a failure
      throw new AssertionError("http response mono returned empty");
    }
    String json = response.getBodyAsString().block();
    if (json == null) {
      // this shouldn't happen, the mono should complete with a response or a failure
      throw new AssertionError("response body mono returned empty");
    }

    MetadataInstanceResponse metadataInstanceResponse;
    try {
      metadataInstanceResponse = jsonAdapter.fromJson(json);
    } catch (IOException e) {
      logger.debug(
          "Shutting down AzureMetadataService scheduler:"
              + " error parsing response from Azure Metadata Service: {}",
          json,
          e);
      scheduledExecutor.shutdown();
      return;
    }

    updateMetadata(metadataInstanceResponse);
  }
}
