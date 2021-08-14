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

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import org.junit.jupiter.api.Test;

public class QuickPulseIntegrationTests extends QuickPulseTestBase {
  private QuickPulsePingSender getQuickPulsePingSender() {
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.setConnectionString("InstrumentationKey=testing-123");
    return new QuickPulsePingSender(
        getHttpPipeline(), telemetryClient, "machine1", "instance1", "qpid123");
  }

  private QuickPulsePingSender getQuickPulsePingSenderWithAuthentication() {
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.setConnectionString("InstrumentationKey=testing-123");
    return new QuickPulsePingSender(
        getHttpPipelineWithAuthentication(), telemetryClient, "machine1", "instance1", "qpid123");
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
}
