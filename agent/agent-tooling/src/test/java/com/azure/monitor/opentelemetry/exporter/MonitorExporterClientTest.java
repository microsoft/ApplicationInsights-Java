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

package com.azure.monitor.opentelemetry.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.monitor.opentelemetry.exporter.implementation.models.ExportResult;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ExportResultException;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test cases for synchronous monitor exporter client. */
public class MonitorExporterClientTest extends MonitorExporterClientTestBase {

  private MonitorExporterClient getClient() {
    return getClientBuilder().buildClient();
  }

  @Test
  public void testSendRequestData() {
    List<TelemetryItem> telemetryItems = getValidTelemetryItems();
    ExportResult exportResult = getClient().export(telemetryItems);

    assertTrue(exportResult.getErrors().isEmpty(), "Empty error list expected.");
    assertEquals(3, exportResult.getItemsAccepted());
    assertEquals(3, exportResult.getItemsReceived());
  }

  @Test
  public void testSendPartialInvalidRequestData() {

    List<TelemetryItem> telemetryItems = getPartiallyInvalidTelemetryItems();

    ExportResult exportResult = getClient().export(telemetryItems);
    assertEquals(3, exportResult.getItemsReceived());
    assertEquals(2, exportResult.getItemsAccepted());
    assertEquals(1, exportResult.getErrors().size());
    assertEquals(1, exportResult.getErrors().get(0).getIndex());
  }

  @Test
  public void testSendAllInvalidRequestData() {
    List<TelemetryItem> telemetryItems = getAllInvalidTelemetryItems();
    Assertions.assertThrows(ExportResultException.class, () -> getClient().export(telemetryItems));
  }
}
