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

package com.microsoft.applicationinsights.agent.internal.quickpulse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.core.http.HttpRequest;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class QuickPulseIntegrationTests extends QuickPulseTestBase {
  private static final String connectionString = "InstrumentationKey=ikey123";
  private static final String instrumentationKey = "ikey123";

  private QuickPulsePingSender getQuickPulsePingSender() {
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.setConnectionString(connectionString);
    return new QuickPulsePingSender(
        getHttpPipeline(), telemetryClient, "machine1", "instance1", "qpid123");
  }

  private QuickPulsePingSender getQuickPulsePingSenderWithAuthentication() {
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.setConnectionString(connectionString);
    return new QuickPulsePingSender(
        getHttpPipelineWithAuthentication(), telemetryClient, "machine1", "instance1", "qpid123");
  }

  private QuickPulsePingSender getQuickPulsePingSenderWithValidator(HttpPipelinePolicy validator) {
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.setConnectionString(connectionString);
    return new QuickPulsePingSender(
        getHttpPipelineWithValidator(validator),
        telemetryClient,
        "machine1",
        "instance1",
        "qpid123");
  }

  @Test
  public void testPing() {
    QuickPulsePingSender quickPulsePingSender = getQuickPulsePingSender();
    QuickPulseHeaderInfo quickPulseHeaderInfo = quickPulsePingSender.ping(null);
    assertThat(quickPulseHeaderInfo.getQuickPulseStatus()).isEqualTo(QuickPulseStatus.QP_IS_ON);
  }

  @Test
  public void testPingWithAuthentication() {
    QuickPulsePingSender quickPulsePingSender = getQuickPulsePingSenderWithAuthentication();
    QuickPulseHeaderInfo quickPulseHeaderInfo = quickPulsePingSender.ping(null);
    assertThat(quickPulseHeaderInfo.getQuickPulseStatus()).isEqualTo(QuickPulseStatus.QP_IS_ON);
  }

  @Test
  public void testPingRequestBody() throws InterruptedException {
    CountDownLatch pingCountDown = new CountDownLatch(1);
    String expectedRequestBody =
        "\\{\"Documents\":null,\"InstrumentationKey\":null,\"Metrics\":null,\"InvariantVersion\":1,\"Timestamp\":\"\\\\/Date\\(\\d+\\)\\\\/\",\"Version\":\"2.2.0-738\",\"StreamId\":\"qpid123\",\"MachineName\":\"machine1\",\"Instance\":\"instance1\",\"RoleName\":null\\}";
    QuickPulsePingSender quickPulsePingSender =
        getQuickPulsePingSenderWithValidator(
            new ValidationPolicy(pingCountDown, expectedRequestBody));
    QuickPulseHeaderInfo quickPulseHeaderInfo = quickPulsePingSender.ping(null);
    assertThat(quickPulseHeaderInfo.getQuickPulseStatus()).isEqualTo(QuickPulseStatus.QP_IS_ON);
    assertTrue(pingCountDown.await(60, TimeUnit.SECONDS));
  }

  @Test
  public void testPostRequest() throws InterruptedException {
    ArrayBlockingQueue<HttpRequest> sendQueue = new ArrayBlockingQueue<>(256, true);
    CountDownLatch pingCountDown = new CountDownLatch(1);
    CountDownLatch postCountDown = new CountDownLatch(1);
    Date currDate = new Date();
    String expectedPingRequestBody =
        "\\{\"Documents\":null,\"InstrumentationKey\":null,\"Metrics\":null,\"InvariantVersion\":1,\"Timestamp\":\"\\\\/Date\\(\\d+\\)\\\\/\",\"Version\":\"2.2.0-738\",\"StreamId\":\"qpid123\",\"MachineName\":\"machine1\",\"Instance\":\"instance1\",\"RoleName\":null\\}";
    String expectedPostRequestBody =
        "\\[\\{\"Documents\":null,\"InstrumentationKey\":\""
            + instrumentationKey
            + "\",\"Metrics\":\\[\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Requests\\\\\\/Sec\",\"Value\":0,\"Weight\":1\\},\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Request Duration\",\"Value\":0,\"Weight\":0\\},\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Requests Failed\\\\\\/Sec\",\"Value\":0,\"Weight\":1\\},\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Requests Succeeded\\\\\\/Sec\",\"Value\":0,\"Weight\":1\\},\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Dependency Calls\\\\\\/Sec\",\"Value\":0,\"Weight\":1\\},\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Dependency Call Duration\",\"Value\":0,\"Weight\":0\\},\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Dependency Calls Failed\\\\\\/Sec\",\"Value\":0,\"Weight\":1\\},\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Dependency Calls Succeeded\\\\\\/Sec\",\"Value\":0,\"Weight\":1\\},\\{\"Name\":\"\\\\\\\\ApplicationInsights\\\\\\\\Exceptions\\\\\\/Sec\",\"Value\":0,\"Weight\":1\\},\\{\"Name\":\"\\\\\\\\Memory\\\\\\\\Committed Bytes\",\"Value\":266338304,\"Weight\":1\\},\\{\"Name\":\"\\\\\\\\Processor\\(_Total\\)\\\\\\\\% Processor Time\",\"Value\":\\d+,\"Weight\":1\\}\\],\"InvariantVersion\":1,\"Timestamp\":\"\\\\\\/Date\\(\\d+\\)\\\\\\/\",\"Version\":\"java:3\\.2\\.0-BETA\\.3-SNAPSHOT\",\"StreamId\":null,\"MachineName\":\"machine1\",\"Instance\":\"instance1\",\"RoleName\":null\\}\\]";
    QuickPulsePingSender pingSender =
        getQuickPulsePingSenderWithValidator(
            new ValidationPolicy(pingCountDown, expectedPingRequestBody));
    QuickPulseHeaderInfo quickPulseHeaderInfo = pingSender.ping(null);
    QuickPulseDataSender dataSender =
        new QuickPulseDataSender(
            getHttpPipelineWithValidator(
                new ValidationPolicy(postCountDown, expectedPostRequestBody)),
            sendQueue);
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.setConnectionString(connectionString);
    QuickPulseDataFetcher dataFetcher =
        new QuickPulseDataFetcher(sendQueue, telemetryClient, "machine1", "instance1", null);
    QuickPulseDataCollector.INSTANCE.enable(telemetryClient);
    final long duration = 112233L;
    TelemetryItem telemetry =
        createRequestTelemetry("request-test", currDate, duration, "200", true);
    telemetry.setInstrumentationKey(instrumentationKey);
    QuickPulseDataCollector.INSTANCE.add(telemetry);
    QuickPulseCoordinatorInitData initData =
        new QuickPulseCoordinatorInitDataBuilder()
            .withDataFetcher(dataFetcher)
            .withDataSender(dataSender)
            .withPingSender(pingSender)
            .withWaitBetweenPingsInMillis(10L)
            .withWaitBetweenPostsInMillis(10L)
            .withWaitOnErrorInMillis(10L)
            .build();
    QuickPulseCoordinator coordinator = new QuickPulseCoordinator(initData);

    Thread coordinatorThread = new Thread(coordinator, QuickPulseCoordinator.class.getSimpleName());
    coordinatorThread.setDaemon(true);
    coordinatorThread.start();

    Thread senderThread = new Thread(dataSender, QuickPulseDataSender.class.getSimpleName());
    senderThread.setDaemon(true);
    senderThread.start();
    Thread.sleep(100);
    assertTrue(pingCountDown.await(1, TimeUnit.SECONDS));
    assertThat(quickPulseHeaderInfo.getQuickPulseStatus()).isEqualTo(QuickPulseStatus.QP_IS_ON);
    assertTrue(postCountDown.await(1, TimeUnit.SECONDS));
    senderThread.interrupt();
    coordinatorThread.interrupt();
  }
}
